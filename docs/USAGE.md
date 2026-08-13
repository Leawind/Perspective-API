# 各场景使用方式

> 本文参照[设计草图](./DESIGN.md)和[使用场景](./USE_CASES.md)，分别简单分析各场景下分别应该如何使用 Perspective API 实现相关功能。
>
> 本文的主要目的是利用各种使用场景探究当前设计草图是否足够合理。
>
> - 仅做宏观分析，不写具体代码
> - 本文仅供参考，不代表最佳实现途径
> - 不意味着相关模组应当立即迁移到 Perspective API
> - 不必在更新设计草图或使用场景时立即更新本文
>
> 在更新本文内容时：
>
> - 如果一个模组不适合使用视角API，请跳过它
> - 如果一个模组适合使用视角API，但当前设计草图中的设计无法满足，或设计不合理导致其用法不够优雅，请说明情况，不要修改设计草图

下文只列出适合使用 Perspective API 的功能。未出现的模组或特性表示它们目前不需要使用 Perspective API，或者现有资料不足以说明它们应当使用。

## 判断方式

一个功能是否应当使用 Perspective API，首先取决于它是否参与相机状态控制，而不是它是否与相机或渲染有关。

| 功能行为                               | 使用方式                 |
| -------------------------------------- | ------------------------ |
| 决定基础相机位置、旋转、FOV 或投影     | Perspective              |
| 临时替换玩家当前选择的基础视角         | Perspective + 临时覆盖   |
| 在任意基础视角上协作式叠加相机效果     | Modifier                 |
| 只读取最终相机状态或原版 `CameraType`  | 直接读取原版对象         |
| 读取上一次相机更新的最终状态           | Perspective API 历史快照 |
| 处理输入、HUD、截图、camera entity 等  | 由使用方自行实现         |
| 生成额外渲染通道或从主相机派生其他视图 | 由使用方自己的渲染器实现 |

一个模组可以同时使用多种方式。例如拍照模组可以用 Perspective 控制取景器的基础视角，用 Modifier 实现独立的缩放效果，同时继续自行处理 HUD、鼠标灵敏度和截图。

### 选择 Perspective 或 Modifier

当功能需要持续保存自己的相机位置、决定完整相机路径、切换原版 `CameraType`，或者在启用期间替换玩家选择的视角时，应使用 Perspective。

当功能只需要在当前基础视角上增加偏移、旋转或 FOV 变化，并且应当与其他 Perspective 共同工作时，应使用 Modifier。Modifier 不应用于把任意基础视角强行改造成另一种完整视角。

### 临时功能

通过物品使用、按键或游戏事件临时启用的基础视角，通常注册为不可切换 Perspective，并通过临时覆盖控制其是否生效。

临时覆盖结束后，Perspective API 会重新解析下面仍然有效的选择。调用方不需要记录进入临时视角前的 Perspective，也不应直接保存和恢复原版相机字段。

### 不属于相机状态的部分

Perspective API 只协调相机状态。一个 Perspective 或 Modifier 可以读取使用方自己的输入和游戏状态，但下列行为仍由使用方自行实现：

- 阻止或重定向玩家输入
- 调整鼠标灵敏度
- 修改移动方向、攻击方向或交互射线
- 隐藏 HUD、准星、手部或玩家模型
- 切换 camera entity
- 截取、编码或保存渲染结果

这些功能可以查询当前 Perspective，以确保只在相关视角生效。

### 上一次最终相机状态

需要从上一帧最终画面开始生成连续相机路径时，使用 `PerspectiveAPI.getPreviousCameraState()` 取得上一次完成的主相机更新结果。该状态已经包含 Perspective、Modifier 和 Perspective 切换过渡的结果，不应改用当前 Perspective 的目标状态代替。

该方法在尚无有效快照时返回 `null`。每次返回的 `PerspectiveState` 都是独立的只读快照，可以由调用方保留并在后续帧继续使用。只需要读取当前帧结果的功能仍应在合适的 render tick 阶段直接读取原版 `Camera`。

## 假想的模组特性

### 潜望镜

潜望镜视角应注册为不可切换 Perspective，并选择与其所需原版行为对应的 `BaseType`。Perspective 在每个渲染帧中设置潜望镜的位置、旋转和 FOV。

使用潜望镜物品时，模组使临时覆盖返回该 Perspective；停止使用后让覆盖失效。玩家原先选择的 Perspective 会自动重新生效。

潜望镜遮罩、物品使用状态和鼠标控制不属于相机状态，由模组自行实现。

### 自定义第三人称视角

每个玩家预设可以作为一个运行时注册的、可切换 Perspective：

- 每个预设拥有稳定且唯一的 ID
- 每个 Perspective 读取自己对应的距离、偏移、旋转方式和 FOV 参数
- 创建或删除预设时注册或注销对应 Perspective
- 修改预设参数时更新 Perspective 自己读取的配置，不动态修改 trait

内置 Perspective Switcher 会把这些 Perspective 作为独立候选项交给玩家选择。模组可以默认不注册任何自定义预设，或者只注册一个默认预设。

如果多个预设只是参数不同，可以复用同一种实现类型，但每个注册仍然需要独立的行为实例和元数据。

### 上下文中的多个视角选择

如果一个功能允许玩家创建多个对象，并让每个对象分别记住当前视角，可以让这些对象与 Perspective API 持有的玩家选择保持同步。

功能订阅 `PerspectiveSelection` 的变化事件，在实际选择发生变化时更新当前对象指向的 Perspective。切换当前对象时，先更新当前对象引用，再把新对象保存的 Perspective 设置为玩家选择；如果 ID 未变化，不会产生重复事件。

内置 Perspective Switcher 和自定义条件逻辑都直接修改同一个 `PerspectiveSelection`。变化事件由 `PerspectiveSelection` 发布，而不是由 Perspective Switcher 发布，因此该功能不与玩家交互方式耦合。Perspective API 负责持久化当前玩家选择。

### 锁定视角

锁定的目标是让目标实体保持在画面中，而不是直接取得相机旋转的控制权。该功能不应实现为 Modifier，而应通过注入游戏的鼠标视角移动事件形成闭环控制。

按键按下时，模组自行从原版相机和画面信息中选择目标。存在有效目标时，根据目标在画面中的位置调整鼠标事件的移动量；当前 Perspective 随后按照自己的控制逻辑转动视角。下一帧再根据新的画面位置继续调整，直到目标稳定地位于画面内。

调整输入前必须先判断当前 Perspective 是否支持这种控制。可以使用两种途径：

- 根据当前 Perspective 的 ID，识别锁定模组明确适配的已知视角
- 查询当前 Perspective 是否声明共享 trait `controllable`

ID 检测适合处理已知 Perspective 的特殊控制语义；`controllable` 则允许未被锁定模组预先认识的 Perspective 声明兼容能力。声明了 `controllable` 的 Perspective 必须遵守设计草图中记录的输入响应契约。

Trait 只说明 Perspective 本身是否支持鼠标控制。锁定模组还需要检查菜单、鼠标捕获等当前游戏状态。如果既不是已知支持的 ID，也没有 `controllable` trait，或者当前无法接收视角移动输入，则不应修改相机状态或鼠标事件，而应在屏幕上显示文本，向玩家说明当前视角无法锁定。

`controllable` 不保证当前输入链没有被其他模组临时重映射。锁定模组应根据后续画面反馈判断控制是否实际生效；目标位置没有按预期响应或控制无法收敛时，应停止注入并向玩家说明当前输入状态不支持锁定。

目标搜索、攻击判定、按键和锁定状态均由模组自行管理。

### 正交俯视视角

正交俯视视角应实现为可切换 Perspective。它负责：

- 设置俯视玩家的相机位置和旋转
- 将投影模式设置为正交投影
- 根据玩家与光标的距离动态设置正交视野高度

光标渲染、屏幕坐标到世界坐标的换算、点击地面和自动寻路不属于 Perspective API。

## 第三人称相机

### Shoulder Surfing Reloaded

越肩相机本身适合作为一个可切换 Perspective。肩位、相机距离和位置偏移作为该 Perspective 的内部参数，由它产生完整的第三人称基础状态。

自由观察和玩家移动方向解耦需要模组自行处理输入与玩家朝向，但可以根据当前 Perspective 判断这些逻辑是否生效。

动态准星和玩家透明度只消费最终相机状态或修改其他渲染结果，不需要通过 Perspective API 实现。

### Leawind's Third Person

第三人称基础相机应实现为可切换 Perspective。自由观察、偏移、距离、瞄准模式和相机平滑共同参与该 Perspective 的目标状态计算。

如果玩家可以创建多个完整预设，可以像“自定义第三人称视角”场景一样，为每个预设注册独立 Perspective；如果左、中、右构图只是同一视角内的临时参数，也可以保留为一个 Perspective 的内部状态。

移动方向转换、射击方向修正、准星和玩家透明度仍由模组自行实现。需要使用最终相机位置执行射线检测时，可以在相机状态写回后直接读取原版 `Camera`。

## 传送、回放与电影镜头

### Grand Teleport

Grand Teleport 的完整高空转场决定了基础相机路径，应实现为不可切换 Perspective，并通过传送期间有效的临时覆盖激活。

该 Perspective 应禁止普通进入和离开过渡，因为起飞、水平移动和下降等阶段已经组成完整的自定义动画。位置、旋转和 FOV 由 Perspective 根据传送阶段计算。

为从上一帧最终画面平滑开始，模组应在转场开始时通过 `PerspectiveAPI.getPreviousCameraState()` 取得上一次完成的主相机更新结果。新 Perspective 以该快照作为自定义路径的起点，而不假定改变 `BaseType` 后收到的原版初始状态仍等于上一帧画面。

跨维度转场期间，覆盖注册保持存在，并根据模组自己的传送状态继续返回该 Perspective。换世界只触发重新求值，不应要求重新注册覆盖。

阻止玩家移动和相机输入由 Grand Teleport 自行实现，并只在转场 Perspective 生效时启用。

### ReplayMod

回放摄像机适合作为程序化激活的 Perspective。自由移动、跟随实体和关键帧路径都是该 Perspective 的内部相机模式，共同产生位置和包含 roll 的旋转。

回放时间和关键帧插值由 ReplayMod 自己管理，不应使用 Perspective API 的普通切换过渡代替。进入和退出回放摄像机时可以选择禁用普通切换过渡。

立体、立方体和等距柱状视频属于从基础相机路径派生渲染通道，不应注册为 Perspective，也不应扩展为普通投影模式。

### Flashback

用于回放编辑的电影摄像机可以作为不可切换 Perspective，在进入回放或相机编辑模式时通过临时覆盖激活。

镜头时间轴、关键帧编辑和回放状态由 Flashback 管理；Perspective 只负责把当前时间对应的镜头状态写入基础相机。

## 相机旋转与晃动

### Do a Barrel Roll

Do a Barrel Roll 适合把相机状态相关的部分实现为 Modifier，但它的输入控制和相机状态合成应当分开处理。

该模组当前会在鞘翅飞行期间接管原版的鼠标视角移动输入，按照三轴飞行规则计算新的朝向，然后通过原版的实体视角转动逻辑把 yaw 和 pitch 写回玩家实体。roll 则作为独立状态保存在玩家实体上。因此，基础 Perspective 不会因为 API Modifier 而再次消费同一份鼠标输入；跟随 camera entity 朝向的 Perspective 会自然取得已经更新的 yaw 和 pitch。

Do a Barrel Roll 应继续自行负责以下内容：

- 拦截和重映射鼠标与按键输入
- 根据输入、速度和配置计算三轴旋转增量
- 更新玩家实体的 yaw、pitch 和独立 roll 状态
- 同步实体 roll，并处理与飞行物理、玩家模型和 HUD 相关的行为

Modifier 只读取活动 camera entity 上插值后的 roll，并把相应的局部滚转合成到当前相机四元数中。它不应再次应用 yaw 或 pitch，也不应修改位置、FOV 或投影。退出滚转后的回正效果仍属于该 Modifier 负责的视觉 roll 状态。

Modifier 在 Perspective 切换过渡之前执行，因此包含 roll 的完整目标状态会参与基础视角切换，并从切换前最终实际采用的状态连续过渡。Modifier 应使用活动 camera entity，而不是硬编码本地玩家，并根据当前相机与实体的实际朝向关系组合旋转，不能根据 `BaseType` 推断前后视角的具体行为。

迁移到 Perspective API 时，Do a Barrel Roll 现有的相机 roll 注入和 API Modifier 不能同时应用，否则原版初始相机状态中已经存在的 roll 会被重复叠加。它可以保留输入、实体状态和其他渲染逻辑，只用 Modifier 替代把 roll 写入主相机的部分。

源码中名为 `EARLY_CAMERA_MODIFIERS` 和 `LATE_CAMERA_MODIFIERS` 的事件修改的是写入玩家实体之前的输入旋转增量，不是 Perspective API 对最终相机状态执行的 Modifier，不应将两者视为同一层抽象。

如果某个 Perspective 完全忽略 camera entity 的 yaw 和 pitch，并拥有自己的输入与朝向状态，Do a Barrel Roll 对玩家实体的控制未必会改变该 Perspective 的观察方向。这属于输入语义兼容问题，不是使用 Modifier 导致的状态冲突；当前 `controllable` trait 也不足以承诺 Perspective 跟随 camera entity 的旋转。

### Camera Overhaul

Camera Overhaul 可以把不同性质的效果拆成多个 Modifier：

- yaw 和 pitch 平滑 Modifier 将当前相机方向作为目标，并自行维护平滑后的方向
- 行走、疾跑、跳跃等运动产生的 pitch 或 roll 使用独立 Modifier
- 空闲摇摆和世界位置相关的相机震动使用独立 Modifier

这些 Modifier 都在 Perspective 切换过渡前执行。拆分后，每种效果可以拥有独立 ID、优先级和启用条件，其他相机模组也能在它们之间安排自己的修正。

玩家移动状态、震动事件和参数平滑由 Camera Overhaul 自行维护。空间震动可以使用 Modifier 收到的当前相机位置计算距离衰减。

## FOV

### Zoomify

Zoomify 只改变当前视角的 FOV，适合作为 Modifier。玩家切换 Perspective 时，正在使用的缩放倍率仍然完整作用于切换后的画面。

Modifier 只修改 FOV，不改变位置、旋转或投影模式。滚轮控制、缩放动画目标和鼠标灵敏度由 Zoomify 自行管理。

如果缩放需要限制在某些 Perspective 中，可以根据当前 Perspective 的 trait 或模组配置决定 Modifier 是否生效。

## 取景与拍照

### Photography

由于取景器要求第一人称基础视角，它适合作为不可切换 Perspective，并在玩家使用相机物品时通过临时覆盖激活。Perspective 设置相应 `BaseType` 和取景 FOV，但不需要修改未涉及的位置或旋转。

如果缩放效果还需要在其他 Perspective 中复用，可以把 FOV 缩放独立为 Modifier；否则也可以由取景器 Perspective 内部计算。

隐藏 HUD、调整鼠标灵敏度、读取主渲染目标和生成地图图像都不属于 Perspective API。

### Exposure

Exposure 中适合使用 Perspective API 的功能应分别处理：

- 手持相机取景器：不可切换的第一人称 Perspective
- 自拍模式：不可切换的正面第三人称 Perspective
- 镜头焦距缩放：取景器内部状态，或仅在相应模式生效的 Modifier

这些 Perspective 在对应拍摄模式中通过临时覆盖激活，退出模式后恢复玩家原来的选择。

相机架模式仍由 Exposure 自行切换 camera entity。切换完成后，如果它还需要由 Perspective API 控制该实体对应的基础相机状态，可以继续使用临时 Perspective；Perspective API 本身不管理 camera entity。

临时渲染照片所需的后处理、滤镜、gamma、渲染目标和额外拍摄通道由 Exposure 自行实现。只有真正影响玩家当前基础相机的那部分状态才进入 Perspective API 管线。

## 自由相机与相机工具

### Camera Utils

Camera Utils 的不同功能适合使用不同机制：

- 按键或滚轮缩放：Modifier
- 固定当前位置和旋转：不可切换 Perspective，通过按键控制的临时覆盖激活
- 长距离第三人称相机：可切换 Perspective
- 两个第三人称预设：两个独立的可切换 Perspective
- 只作用于 Camera Utils 视角的电影平滑：对应 Perspective 的内部计算

返回上一次固定机位所需的位置和旋转由 Camera Utils 自己保存。相机固定时停用原版视角晃动属于原版行为控制，不由 Perspective API 统一处理。

### Freecam

Freecam 应实现为保存独立位置和旋转的 Perspective。通过按键启用时，临时覆盖选择该 Perspective；退出后恢复玩家原来的 Perspective。

Freecam 可以注册为不可切换 Perspective，使其只能由自己的启用按键控制；如果希望玩家从内置 Perspective Switcher 中选择，也可以声明为可切换。

阻止真实玩家移动、重定向鼠标输入和处理自由相机移动速度都由 Freecam 自己实现。Perspective 只负责把已计算的位置、旋转和可选 FOV 写入相机状态。

## 对设计草图的验证

上述适合进入相机状态管线的功能主要由以下三种核心机制覆盖：

- Perspective 负责独占的基础相机状态
- 临时覆盖负责在不丢失玩家选择的前提下临时切换 Perspective
- Modifier 负责在 Perspective 切换过渡前合成可组合的相机效果

输入、HUD、camera entity、截图和额外渲染通道继续由各模组自行实现，不需要为了覆盖这些附属功能扩大相机状态 API。

Grand Teleport 所需的上一帧最终状态由 Perspective API 的历史快照方法直接提供，不要求模组自行选择 render tick 时机并从原版 `Camera` 缓存。特殊视频投影仍然可以在模组自己的渲染器中从基础相机派生，不要求扩展单个相机状态的投影模式。

锁定视角可以利用 `controllable` trait 判断当前 Perspective 是否适合通过鼠标事件进行闭环控制，但输入注入和运行时状态判断仍由锁定模组实现。

Do a Barrel Roll 可以在 API 之外完成输入重映射和实体朝向更新，再通过 Modifier 只合成相机 roll。输入处理与 Modifier 不会因此重复消费鼠标输入，也不需要把完整飞行相机实现为临时 Perspective。对于完全不跟随 camera entity 朝向的 Perspective，其输入语义仍需要单独协调。

按照当前宏观分析，设计草图能够覆盖本文列出的适用场景。具体公共接口、共享 trait、旋转组合约定和优先级约定仍需要在实现阶段验证。
