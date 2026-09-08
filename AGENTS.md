# Perspective API Agent 指南

## 最高约束

最高约束由用户（人类开发者）亲自编写，Agent 不得擅动。

- 目标：提供相关机制和接口，让各种涉及相机状态控制权的模组互相兼容。
- 涉及到相机状态的 Minecraft 版本差异应由本模组内部处理
- 不为特定模组编写专用的兼容逻辑
- Perspective API 仍处于 Beta 阶段，允许进行大规模重构，并应彻底删除废弃的类、方法、字段、测试和文档，不为旧接口保留兼容层

## 其他

### 文档和 Agent 指南

Agent 指南文件分工：

- `AGENTS.md`（本文件，入库）：项目约束、编码指南与踩坑记录
- `AGENTS-LOCAL.md`（本地，不纳入版本管理）：本机开发环境相关的经验，建议阅读
- `AGENTS-NOTES.md`（本地，不纳入版本管理）：有时效性的调查快照，不是设计文档

文档：

- 本仓库中的文档只有 README.md 是面向用户（玩家、API使用者）的，需要同时包含英语和中文，其他文档一律使用简体中文（中国大陆）
- 本模组的设计细节位于 `docs/DESIGN.md`，docs/ 下其他文件通常不需要访问，仅在用户明确要求时作为参考。
- 仓库中的文档代表目标，可超前于代码实现，不可滞后
- Agent 工作时应先更新文档，再编写实现
- 如果执行任务时发现现有文档存在错误、矛盾、过时、缺失等问题而应当修改，但用户没预料到，应向用户请示，不能编写超前于文档的实现或擅自修改文档
- 执行任务时应适时提交更改，而非完成全部任务后一次性提交
- 若踩到值得记录的坑，可在任务完成后报告

## 约束

### CameraType

BaseType 与原版的 CameraType 枚举一一对应。

Minecraft 中有许多机制都会根据当前 CameraType 有不同的行为，包括但不限于第一人称手部渲染、本玩家的玩家实体渲染、望远镜UI渲染等。
这些行为在不同 Minecraft 版本中很可能存在差异，且无法预测其将来的变化。

也就是说 BaseType 只是为了控制原版中的这类行为，其具体含义是不透明的，本模组不能基于当前 Minecraft 版本的行为细分其功能。

## 编码指南

- 公共 API 中的返回类型、参数类型必须拥有 Nullability 注解，除非是基本类型
  - 对于标注了 `@NonNull` 的方法参数，当传入 null 时应该抛出 `NullPointerException`
- 接口和实现类方法的 Nullability 注解要保持一致
- 简单的卫语句可以不使用花括号：`if (condition) return;`

### 代码风格

#### 格式化

- Java
  - 遵循 `google-java-format` 规范（非强制要求，不要调用它进行格式化）
  - 缩进：2 个空格
  - 大括号：K&R 风格（左括号不换行）
  - 导入语句：禁止使用通配符导入（如 `import java.util.*`）
- md, yml, json, toml 等文件用 deno 进行格式化：`deno fmt`

#### Javadoc 风格

使用 `///` 风格，类似 markdown 语法。

不要使用：

- `<p>` 标签
- `{@code XXX}` 格式

可以使用：

- 反引号行内代码
- `{@link Xxx}` 引用类或成员
- `@param xxx` 描述参数

方法签名或参数类型本身已经能表达的约束，不要在 Javadoc 中重复警告；只有类型系统无法表达的约束（如对象生命周期、线程要求）才需要写成文档。

### 命名规范

- 表示角度或弧度的参数和变量名要用后缀表示其单位：`Deg` 是角度，`Rad` 是弧度
- Mixin 类以 `Mixin` 为后缀，例如 `MinecraftMixin`

### Stonecutter 指南

当前激活的 Minecraft 版本可以在 `stonecutter.gradle.kts` 文件中找到。

在代码中可以根据 Minecraft 版本、加载器等条件编译代码，示例：

```java
/*? if >=26.1 {*/
@Unique private static final String SETUP_CAMERA_METHOD = "alignWithEntity";
/*? } else {*/
/*@Unique private static final String SETUP_CAMERA_METHOD = "setup";
*//*? } */
```

不符合当前条件的代码使用 `/*  */` 包裹。

尽量避免深层嵌套，如果必须嵌套，符合当前条件的代码中的条件仍然用 `/* */`，被注释的其他版本的代码中的条件用 `/^ ^/`。

例如，如果当前版本是 26.1 或以上：

```java
/*? if >=26.1 {*/
@Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"))
private void beforeCameraUpdate(float partialTicks, CallbackInfo ci) {
  Camera camera = (Camera) (Object) this;
  cameraSetupContext.setup(camera, partialTicks);
}
/*? } else {*/
  /*@Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"))
  private void beforeMoveCamera(
    /^? if >= 1.21.11 {^/
    net.minecraft.world.level.Level blockGetter,
    /^? } else {^/
    /^net.minecraft.world.level.BlockGetter blockGetter,
    ^//^? }^/
    net.minecraft.world.entity.Entity entity,
    CallbackInfo ci) {
    cameraSetupContext.setup((Camera) (Object) this, partialTicks);
  }
*//*? } */
```

如果当前版本是 1.21.11：

```java
/*? if >=26.1 {*/
/*@Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"))
private void beforeCameraUpdate(float partialTicks, CallbackInfo ci) {
  Camera camera = (Camera) (Object) this;
  cameraSetupContext.setup(camera, partialTicks);
}*//*? } else {*/
  @Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"))
  private void beforeMoveCamera(
    /*? if >= 1.21.11 {*/
    net.minecraft.world.level.Level blockGetter,
    /*? } else {*/
    /*net.minecraft.world.level.BlockGetter blockGetter,
    *//*? }*/
    net.minecraft.world.entity.Entity entity,
    CallbackInfo ci) {
    cameraSetupContext.setup((Camera) (Object) this, partialTicks);
  }
/*? } */
```

#### 风格

- 当需要使用 else、else-if 时，尽量用 `>=` 条件，不要用 `<` 或 `<=`
- 如果一个方法体中使用了 Stonecutter 条件编译，且需要通过注释说明为什么需要条件编译，应将该注释写在方法的 Javadoc 中，而不是方法体中

正确示例：

```java
/*? if >=1.21.11 {*/
return currentVersion().dataVersion().version();
/*? } else {*/
/*return currentVersion().getDataVersion().getVersion();
 *//*? }*/
```

错误示例：

```java
/*? if <1.21.11 {*/
/*return currentVersion().getDataVersion().getVersion();
 *//*? } else {*/
return currentVersion().dataVersion().version();
/*? }*/
```

#### 格式化提示

合并相邻的条件编译块时（即 `*/` 后紧跟 `/*?`），应确保它们紧邻而非被空白分隔：

```
查找：(\s|^)\*/(\s|\n)+/\*\?
替换：*//*?
```

## Minecraft 版本兼容性

当一个构建产物兼容连续的多个 Minecraft 版本时，应以其中最低版本作为开发和构建目标。模组元数据中的 Minecraft 版本要求只声明为 `>=` 该最低版本，不声明上界；这是项目有意采用的兼容性策略，不应为不同变体补充 `<` 上界。发布平台上的额外版本标签则在对应变体的 `gradle.properties` 中通过 `publish.additionalMcVersions` 声明。

## 依赖管理

依赖版本（fabricApi、YACL、ModMenu、各加载器等）在各 `versions/*-*/gradle.properties` 中手动管理。这是有意采用的策略，不引入 Renovate、Dependabot 等自动化依赖更新工具。

## 架构与依赖约束

本项目采用严格的分层架构，各层职责与依赖方向必须严格遵守，严禁反向依赖。

### 包依赖方向

`api` ➜ `logic` / `impl` ➜ `bridge` ➜ `utils`

注：`api` 层原则上不依赖 `internal`，但允许依赖 `bridge` 层中无状态的、纯粹用于构建跨版本基础类型（如 `Identifier`）的工具方法，例如 `Bridge.createIdentifier`。

### 核心约束

1. `bridge` 层禁令：`bridge` 包（包含所有 Mixin）严禁直接 import 或调用 `logic`、`impl` 或 `api` 包中的业务类
2. 事件驱动解耦：`bridge` 层的 Mixin 仅负责拦截原版调用，并发射通用事件（Event）；`logic` 层负责监听这些事件并执行具体业务
3. `logic` 层无宏化：`logic` 包和 `api` 包应尽可能保持 100% 无 Stonecutter 条件编译宏，所有 Minecraft 版本差异必须下沉并封装在 `bridge` 层

### Mixin 约束

1. 禁止使用 `@Redirect`：它会排他地占用调用点，容易与其他模组冲突；优先使用可组合的注入器
2. 禁止使用 `@ModifyArgs`（以及注入 `Args` 参数）：Mixin 0.8.5 会在 `org.spongepowered.asm.synthetic.args` 动态生成 `Args$N`，而 Forge 1.20.1 的 ModLauncher 无法加载该包，造成目标类链接时的 `NoClassDefFoundError`。需要修改多个参数时，使用多个 `@ModifyArg` 或合适的 MixinExtras 注入器

### 强制机制

上述分层约束由两层检查共同强制：

- 根项目的 `checkArchitecture` 任务：文本级检查（import 语句、Stonecutter 宏、Mixin 注入器注解），所有变体的 `check` 任务和 `buildAndCollect` 都会运行
- `InternalArchitectureTest`（ArchUnit）：字节码级检查，能发现绕过 import 语句的全限定名引用，随 `test` 任务运行

## 工作流指南

### Git 提交信息规范

本项目使用自定义 Deno 发布脚本，根据 Conventional Commits 分析版本变化，提交信息必须严格遵守以下规范。

每条提交信息由 header、body 和 footer 组成，全部使用英语编写，结构如下：

```
<header>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

#### Header

格式：`<type>(<scope>): <short summary>`

type 必填，scope 可选。

#### type

type 必须从以下列表中选择，发布脚本会根据 type 决定是否发布新版本：

- feat：新功能
- i18n：语言文件更新
- fix：错误修复
- perf：提升性能的代码变更
- refactor：既不修复错误也不添加功能的代码变更
- test：添加或修正测试
- build：影响构建系统或外部依赖的变更
- ci：影响 CI 配置文件和脚本的变更
- docs：仅文档变更

包含破坏性变更时，必须在 footer 中添加 `BREAKING CHANGE`，详见下方"破坏性变更检查"。

Beta 阶段的版本格式为 `1.<minor>.<patch>-beta`：

- major 始终为 `1`
- 破坏性变更使 minor 加一，并将 patch 重置为零
- `feat`、`i18n`、`fix`、`perf` 和 `revert` 使 patch 加一
- 其他类型默认不触发发布，但包含破坏性变更时仍会触发发布

正式版本从 `release` 分支发布，使用常规语义化版本规则：

- 破坏性变更使 major 加一，并将 minor、patch 重置为零
- `feat` 使 minor 加一，并将 patch 重置为零
- `i18n`、`fix`、`perf` 和 `revert` 使 patch 加一
- `docs`、`ci`、`refactor` 等其他类型不触发发布
- 第一次正式发布直接将最新可达 Beta 版本的核心版本晋升为正式版，例如
  `1.3.7-beta` 晋升为 `1.3.7`

#### scope

scope 根据主要受影响的包决定，如果没有合适的就不写：

| package                                            | scope    |
| -------------------------------------------------- | -------- |
| `io.github.leawind.perspectiveapi.platform`        | platform |
| `io.github.leawind.perspectiveapi.api`             | api      |
| `io.github.leawind.perspectiveapi.internal.bridge` | bridge   |
| `io.github.leawind.perspectiveapi.internal.impl`   | impl     |
| `io.github.leawind.perspectiveapi.internal.logic`  | logic    |
| `io.github.leawind.perspectiveapi.internal.utils`  | utils    |
| `AGENTS.md` / `AGENTS-NOTES.md`                    | agents   |

#### short summary 要求

- 使用祈使语气、现在时态，例如用 "add" 而不是 "added" 或 "adds"
- 首字母不大写
- 末尾不加句号

#### 破坏性变更检查（必须）

提交前必须检查是否包含破坏性变更，以下任一情况均视为破坏性变更：

- 删除公共 API 中的方法、类、接口或字段
- 修改公共 API 方法的签名（参数类型、返回类型、异常声明）
- 将公共 API 中的类/接口移动到内部包
- 修改公共 API 方法的行为（不再向后兼容）

标注了 `@ApiStatus.Internal` 或 `@ApiStatus.Experimental` 的成员，以及声明类型标注了其中任一注解的成员，不保证兼容，移除或修改它们**不算**破坏性变更，不要为其添加 `BREAKING CHANGE`。仅当成员及其声明类型均未标注 `@ApiStatus.Internal` 或 `@ApiStatus.Experimental` 时，公共 API 的上述变化才需要在 footer 中添加：

```
BREAKING CHANGE: <破坏性变更简要描述>
```

#### Body

Body 在所有类型的提交中都可以省略，但如果存在，需解释变更的动机。

#### Footer

Footer 可选。当包含破坏性变更时，格式如下：

```
BREAKING CHANGE: <破坏性变更简要描述>
<BLANK LINE>
<详细描述及迁移说明>
```

项目还未正式发布时可以不写迁移说明。

也可以引用相关的 Issue 或 PR，例如：`Fixes #123` 或 `Closes #456`

#### Revert 提交

如果要回退一个之前的提交，header 格式为：

```
revert: <被回退提交的 header>
```

Body 需包含：`This reverts commit <SHA>`，并说明回退原因。

### CI 测试策略

为了节省 CI 资源，push 到 `dev` 不运行单元测试；发布前（push 到 `beta` / `release` 触发发布流程）强制运行；手动触发 workflow 时默认运行（可通过 `run_tests` 输入关闭）。日常开发在本地自行运行测试。

### 分支

- `dev`：默认分支和日常开发分支
- `beta`：Beta 测试版本发布分支
- `release`：正式版本发布分支
- `feat/`：新功能
- `fix/`：Bug 修复
- `chore/`：构建过程或辅助工具的变动
- `test/`：增加测试或修改现有测试

## 踩坑记录

实现中踩过并确认的坑，供后续任务避让；条目应写清现象与结论，不罗列排查过程。

- stonecutter 的 `replacements.string` 是双向替换：条件为 true 时按 `replace(from, to)` 正向替换，为 false 时反向替换。因此源代码写任意一侧的名称都会被替换为正确值，旧版本条件分支中出现的旧类名（或看似未导入的类）不是错误，不要"修复"。建议共享源码统一使用新版本名称（如 `Identifier`、`GuiGraphicsExtractor`）：当前最高版本无需替换，旧版本自动反向替换；两个条件编译块的唯一区别是被替换的类型名时，可以合并为一个块。
