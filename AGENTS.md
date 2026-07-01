重要：本机特定配置（如源码路径、Demo 位置）位于 `AGENTS-LOCAL.md`，如果存在，请务必阅读该文件

# 编码指南

- 公共API中的返回类型、参数类型必须拥有 Nullability 注解，除非是基本类型。
  - 对于标注了 `@NonNull` 的方法参数，当传入 null 时应该抛出 `NullPointerException`
- 接口和实现类方法的 Nullability 注解要保持一致
- 简单的卫语句可以不使用花括号：`if (condition) return;`

### 代码格式

- java
  - 遵循 `google-java-format` 规范（非强制要求）
  - 缩进：2 个空格
  - 大括号：K&R 风格（左括号不换行）
  - 导入语句：禁止使用通配符导入（如 `import java.util.*`）
- md,yaml,json等文件用deno进行格式化：`deno fmt`

## javadoc 风格

使用`///`风格，类似markdown语法

不要使用：

- `<p>` 标签
- `{@code XXX}` 格式

可以使用：

- 反引号 `` 行内代码
- `{@link Xxx}` 引用类或成员
- `@param xxx` 描述参数

### 命名规范

- 表示角度或弧度的参数和变量名要用后缀表示其单位，Deg是角度，Rad是弧度
- Mixin类以Mixin为后缀，例如`MinecraftMixin`

## Stonecutter 条件编译语法

当前激活的Minecraft版本可以在 `stonecutter.gradle.kts` 文件中找到

在代码中可以根据Minecraft版本、加载器等条件编译代码，示例：

```java
/*? if >=26.1 {*/
@Unique private static final String SETUP_CAMERA_METHOD = "alignWithEntity";
/*? } else {*/
/*@Unique private static final String SETUP_CAMERA_METHOD = "setup";
*//*? } */
```

不符合当前条件的代码使用 `/*  */` 包裹。

尽量避免深层嵌套，如果必须嵌套，符合当前条件的代码中的条件仍然用`/* */`，被注释的其他版本的代码中的条件用 `/^ ^/`。

例如，如果当前版本是26.1或以上：

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

如果当前版本是1.21.11：

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

### 风格

当需要使用else，else-if时，尽量用>=条件，不要用<或<=，例如应该这样写：

```java
/*? if >=1.21.11 {*/
return currentVersion().dataVersion().version();
/*? } else {*/
/*return currentVersion().getDataVersion().getVersion();
 *//*? }*/
```

而不要这样写：

```java
/*? if <1.21.11 {*/
/*return currentVersion().getDataVersion().getVersion();
 *//*? } else {*/
return currentVersion().dataVersion().version();
/*? }*/
```

# 工作流指南

## Git 提交信息规范

本项目使用 [semantic-release](https://github.com/semantic-release/semantic-release)，提交信息必须严格遵守以下规范

每条提交信息由 header、body 和 footer 组成，全部使用英语编写，结构如下：

```
<header>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

### Header

Header 格式：

```
<type>(<scope>): <short summary>
```

type 必填，scope 可选。

#### type

type 必须从以下列表中选择，semantic-release 会根据 type 决定版本号变更：

- feat：新功能
- fix：错误修复
- perf：提升性能的代码变更
- refactor：既不修复错误也不添加功能的代码变更
- test：添加或修正测试
- build：影响构建系统或外部依赖的变更
- ci：影响 CI 配置文件和脚本的变更
- docs：仅文档变更

如果有 BREAKING CHANGE，则触发主版本号升级，详见 footer 部分

#### scope

scope 根据主要受影响的包决定，如果没有合适的就不写

| package                                            | scope    |
| -------------------------------------------------- | -------- |
| `io.github.leawind.perspectiveapi.platform`        | platform |
| `io.github.leawind.perspectiveapi.api`             | api      |
| `io.github.leawind.perspectiveapi.internal.bridge` | bridge   |
| `io.github.leawind.perspectiveapi.internal.impl`   | impl     |
| `io.github.leawind.perspectiveapi.internal.logic`  | logic    |
| `io.github.leawind.perspectiveapi.internal.utils`  | utils    |

#### short summary 要求

- 使用祈使语气、现在时态，例如用 "add" 而不是 "added" 或 "adds"
- 首字母不大写
- 末尾不加句号

### Body

Body 在所有类型的提交中都可以省略，但如果存在，需解释变更的动机

如果变更包含破坏性变更，必须在 body 或 footer 中声明 `BREAKING CHANGE`，并提供迁移说明

### Footer

Footer 可选。当包含破坏性变更时，格式如下：

```
BREAKING CHANGE: <破坏性变更简要描述>
<BLANK LINE>
<详细描述及迁移说明>
```

项目还未正式发布时可以不写迁移说明

也可以引用相关的 Issue 或 PR，例如：Fixes #123 或 Closes #456

### Revert 提交

如果要回退一个之前的提交，header 格式为：

```
revert: <被回退提交的 header>
```

Body 需包含：`This reverts commit <SHA>`，并说明回退原因

## 分支

- `main`: 主分支，正式版本发布分支
- `beta`: Beta 测试版本发布分支
- `dev`: 开发分支
- `feat/`: 新功能
- `fix/`: Bug 修复
- `chore/`: 构建过程或辅助工具的变动
- `test/`: 增加测试或修改现有测试
