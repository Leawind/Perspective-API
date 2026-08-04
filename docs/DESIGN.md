# Perspective API 设计草图

> - 本文仅供参考，不代表未来开发计划
> - 本文内容未必与当前实现相符
> - 修改实现时不必更新本文

本文在 [AGENTS.md](../AGENTS.md) 中的设计原则和基本设计之下，记录 Perspective API 目前的具体设计方向。本文不重复定义这些上位约束。

## 职责范围

Perspective API 负责协调会修改、替换或叠加相机状态的功能，避免多个模组各自直接控制相机时互相覆盖。

[USE_CASES.md](./USE_CASES.md) 是设计时的调查材料，不是 Perspective API 必须支持的需求清单。该文档中列出的模组或特性未必适合使用 Perspective API，同一个模组也可能只有部分功能适合使用。

只需要读取最终相机状态或当前原版 `CameraType` 的功能，不需要依赖 Perspective API。它们应当在合适的 render tick 阶段直接从原版 `Camera` 或其他原版状态中读取结果。Perspective API 应确保最终结果正确写回原版对象，而不是另外建立一套供只读消费者使用的平行状态来源。

以下内容目前不属于核心相机状态，不因为与相机有关就自动纳入 Perspective API：

- 玩家移动与输入处理
- 鼠标灵敏度
- HUD、准星、手部和玩家实体的具体渲染规则
- camera entity 的选择与切换
- 截图、视频编码和渲染目标管理
- 传送门、VR、立方体视频等额外渲染通道
- 区块、远景、光影和其他世界渲染逻辑

使用 Perspective API 的模组仍然可以通过原版事件或自己的 Mixin 实现这些功能，并根据当前 Perspective 决定是否启用。

## Perspective

Perspective 表示一种基础视角，是相机状态的基础生产者。任意时刻最多只有一个 Perspective 对相机生效。

原版相机先按照该 Perspective 的 `BaseType` 执行原版设置，Perspective 随后以原版结果作为初始状态，修改相机的位置、旋转、FOV 或投影设置。Perspective 不需要修改所有字段；未修改的字段保留原版结果。

Perspective 可以来自静态声明，也可以在运行时注册。运行时注册允许模组根据玩家数据创建多个同类 Perspective，例如同一种自定义第三人称视角的多个预设。每个预设作为独立 Perspective 注册，并拥有稳定且唯一的 ID。

Perspective 可以声明为：

- 可切换：允许玩家通过 Perspective Switcher 手动选择
- 不可切换：只能通过临时覆盖等程序化方式激活
- 默认 Perspective：没有其他有效选择时的安全回退

`isAvailable()` 只表示一个已注册的 Perspective 当前能否参与选择，不表示它是否已经注册，也不应依赖调用次数或副作用。

## 元数据与 trait

Perspective 的注册元数据在注册期间保持不变。需要增删或替换运行时 Perspective 时，应通过对应注册句柄操作，不由其他模组修改其元数据。

Trait 描述 Perspective 提供者声明的、相对稳定的视角语义：

- trait 使用开放字符串，不使用封闭枚举
- 共享 trait 使用约定一致的非命名空间名称
- 私有 trait 可以使用命名空间
- trait 只能由 Perspective 自身声明
- trait 不能在注册后动态添加或移除
- trait 不描述逐帧变化的相机状态
- trait 不根据 `BaseType` 自动推断，`BaseType` 也不根据 trait 推断

### 约定的共享 trait

无命名空间的共享 trait 是跨模组契约。Perspective API 的文档应集中记录每个共享 trait 的准确含义和声明条件；Perspective 提供者不能只根据名称猜测含义，消费者也只能查询自己理解的 trait。

目前约定以下共享 trait：

| Trait          | Perspective 应当声明的情况                                   |
| -------------- | ------------------------------------------------------------ |
| `first_person` | 主要从 camera entity 自身的观察位置观察世界                  |
| `third_person` | 主要从 camera entity 外部观察 camera entity                  |
| `controllable` | 标准鼠标视角移动输入能够持续、可预测地控制当前画面的观察方向 |

`first_person` 和 `third_person` 只描述 Perspective 自身的观察方式。它们不根据 `BaseType` 自动声明，也不承诺手部、玩家实体或其他原版内容的具体渲染行为。

Perspective 只有在满足以下条件时才应声明 `controllable`：

- 正常游戏状态下，玩家可以通过标准鼠标视角移动输入改变画面的观察方向
- 其他模组在该输入被用于视角控制前调整鼠标事件的移动量时，与 Perspective 配套的控制实现会使用调整后的结果
- 输入与画面方向之间的关系足够稳定，可以通过观察目标在画面中的位置形成闭环控制

固定机位、脚本镜头、自动跟随镜头，以及忽略标准鼠标视角移动输入的 Perspective 不应声明 `controllable`。如果鼠标只用于移动光标、移动相机位置或控制 roll，而不能可靠地控制画面朝向，也不应声明它。

如果一个 Perspective 只在部分内部模式中接受鼠标视角移动输入，而在其他内部模式中忽略同一输入，它也不应声明 `controllable`。需要声明该 trait 时，应将不满足契约的模式拆分为另一个 Perspective，或者保证消费者能够通过不同的 Perspective ID 明确区分。

`controllable` 描述的是 Perspective 的稳定能力，不表示客户端当前一定能够接收视角移动输入。消费者仍需检查菜单、鼠标捕获和其他瞬时游戏状态。该 trait 也不授予独占输入控制权，不负责协调多个同时修改鼠标事件的模组。

## 玩家选择与临时覆盖

玩家通过 Perspective Switcher 选择自己的持久 Perspective。Switcher 只负责玩家选择，不拥有高于其他模组的特殊控制权。

需要临时替换基础视角的功能通过临时覆盖参与选择。有效 Perspective 按以下顺序解析：

1. 按优先级从高到低检查临时覆盖
2. 使用第一个指向已注册且当前可用 Perspective 的覆盖
3. 没有有效临时覆盖时，使用当前 Switcher 选择的 Perspective
4. 仍然无法解析时，使用默认 Perspective

较低优先级的选择在被覆盖期间仍然保留。临时覆盖停止生效后，API 重新解析并恢复下面仍然有效的选择；调用方不需要保存和还原原版相机字段。

每个覆盖注册由返回给调用方的句柄拥有：

- 句柄只能移除自己创建的注册
- 注册在调用方主动移除前持续存在，即使它暂时没有生效
- 换世界或跨维度时可以重新判断覆盖是否有效，但不能静默删除其他模组的注册
- 同优先级覆盖的注册顺序不应用于跨模组协调

## Modifier

Modifier 对当前 Perspective 产生的状态进行协作式变换，但不拥有基础 Perspective 的选择权。适合 Modifier 的功能包括相机晃动、倾斜、滚转角修正、FOV 缩放和目标朝向修正。

需要完全决定相机路径、持续保存独立位置或临时替换基础视角的功能，应实现为 Perspective，而不是通过 Modifier 覆盖整个状态。

Modifier 按优先级依次执行。同优先级使用注册顺序，但不同模组不应依赖相同优先级下的执行顺序。每个 Modifier 应具有稳定 ID，用于诊断冲突和记录错误。

Modifier 分为两个阶段：

- 过渡前：修改 Perspective 的目标状态，其结果参与 Perspective 切换过渡
- 过渡后：修改已经完成过渡的最终视觉状态，不被 Perspective 切换过渡削弱

平滑改变基础视角目标的修正适合在过渡前执行；相机震动、临时 roll 等最终视觉效果通常适合在过渡后执行。

一个功能在 Perspective API 之外处理输入，并不妨碍它同时使用 Modifier。输入处理可以先更新 camera entity 的朝向或使用方自己的状态，Modifier 再把尚未包含在基础状态中的相机效果合成到管线中。两部分必须有明确边界，不能由输入处理和 Modifier 重复应用同一旋转分量。

Modifier 应只修改自己负责的字段，并按照 API 约定的旋转方向、乘法顺序和坐标空间进行组合。多个 Modifier 对同一字段的变换可能与顺序有关；API 不承诺自动理解各模组的高级语义。

## Perspective 切换过渡

普通切换过渡只处理从一个有效 Perspective 切换到另一个 Perspective，不用于实现某个 Perspective 自身的完整相机动画。

过渡连续插值位置、旋转、透视 FOV 和正交视野高度。投影模式离散切换，不在不同投影模式之间插值。

过渡持续时间和混合参数是全局玩家设置。其他模组不应临时修改全局设置后再尝试恢复。Perspective 可以分别决定是否允许进入和离开时使用普通过渡。

需要自行生成完整相机路径的 Perspective 可以禁止普通进入或离开过渡，并在自己的状态计算中实现动画。

## 生命周期

当解析出的有效 Perspective 发生变化时：

1. 旧 Perspective 收到停用通知
2. 更新原版 `CameraType`
3. 新 Perspective 收到激活通知
4. 根据双方是否允许过渡决定是否开始普通切换过渡

只有当前生效的 Perspective 接收 active client tick 和逐帧相机状态回调。

禁用 Perspective API 时，当前 Perspective 应失去相机控制权并收到停用通知；此时查询接口不应继续报告它正在生效。重新启用后重新解析并激活有效 Perspective。

注册表变化、Perspective 可用性变化和 Switcher 选择变化都会触发重新解析。换世界或跨维度本身不删除注册；需要限制在单个世界中的功能应通过自身状态使对应 Perspective 或覆盖失效。

## 相机状态管线

每次原版相机完成基础设置后，Perspective API 按以下顺序处理状态：

1. 读取原版相机的位置和旋转，并取得原版透视 FOV
2. 建立符合 API 坐标约定的初始相机状态
3. 调用当前 Perspective 计算目标状态
4. 校验状态，并回退 Perspective 产生的无效字段
5. 按顺序应用过渡前 Modifier，并在每次调用后校验状态
6. 应用 Perspective 切换过渡
7. 按顺序应用过渡后 Modifier，并在每次调用后校验状态
8. 将最终位置、旋转、FOV 和投影设置写回原版相机与渲染流程
9. 通知当前 Perspective 最终实际采用的状态

只读取相机结果的其他模组在此之后直接读取原版 `Camera`，不需要通过 Perspective API 注册观察者。

## 相机状态约定

- 位置使用世界坐标
- 旋转使用单位四元数，并完整保留 pitch、yaw 和 roll
- API 的零旋转朝向 `+Z`
- 透视 FOV 使用角度，必须有限且位于开区间 `(0, 180)`
- 正交视野高度表示画面的垂直世界跨度，必须有限且不小于 `0.0001`
- 投影模式目前包括透视投影和以相机前向轴为中心的正交投影

Perspective 和每个 Modifier 的调用分别构成错误隔离边界：

- 回调抛出非致命异常时，撤销该回调对状态的全部修改
- 回调只产生部分无效字段时，只回退无效字段
- 一个扩展失败不能阻止后续扩展继续执行
- 最终写入原版相机的状态必须满足上述约定

## 特殊渲染方式

立体、立方体、等距柱状、VR 眼睛视图和传送门嵌套渲染描述的是渲染通道或由基础相机派生出的额外视图，不是单个相机状态的普通投影模式。

当前设计不要求 Perspective API 提供通用的多视图或渲染通道管理。相关模组可以读取原版相机的最终状态，并在自己的渲染流程中派生其他视图。只有将来出现明确的跨模组相机控制权冲突时，才考虑增加通用抽象。
