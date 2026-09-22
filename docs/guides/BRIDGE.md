# Bridge 与跨版本开发指南

修改 `internal/bridge` 包、Mixin 类，或任何含 Stonecutter 条件编译（`/*? */`）的共享源码前，必读本文。

## Stonecutter 条件编译

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

### 风格

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

### 格式化提示

合并相邻的条件编译块时（即 `*/` 后紧跟 `/*?`），应确保它们紧邻而非被空白分隔：

```
查找：(\s|^)\*/(\s|\n)+/\*\?
替换：*//*?
```

## Mixin 约束

1. 禁止使用 `@Redirect`：它会排他地占用调用点，容易与其他模组冲突；优先使用可组合的注入器
2. 禁止使用 `@ModifyArgs`（以及注入 `Args` 参数）：Mixin 0.8.5 会在 `org.spongepowered.asm.synthetic.args` 动态生成 `Args$N`，而 Forge 1.20.1 的 ModLauncher 无法加载该包，造成目标类链接时的 `NoClassDefFoundError`。需要修改多个参数时，使用多个 `@ModifyArg` 或合适的 MixinExtras 注入器

## 架构约束（bridge 相关部分）

完整分层约束见 [AGENTS.md](../../AGENTS.md)，由 `checkArchitecture` 任务与 `InternalArchitectureTest`（ArchUnit）强制。与 bridge 直接相关的部分：

- `bridge` 包（包含所有 Mixin）严禁直接 import 或调用 `logic`、`impl` 或 `api` 包中的业务类
- Mixin 仅负责拦截原版调用，并发射通用事件（Event）；`logic` 层负责监听这些事件并执行具体业务
- `logic` 包和 `api` 包应尽可能保持 100% 无 Stonecutter 条件编译宏，所有 Minecraft 版本差异必须下沉并封装在 `bridge` 层

## 踩坑记录

收录标准：删掉这条，Agent 会再踩一次吗？不会就不收。淘汰时机：相关代码被删除或行为验证已变化时，随对应任务的提交一并清理。

### stonecutter 的 replacements.string 是双向替换（2026-09-20）

`replacements.string` 是双向替换：条件为 true 时按 `replace(from, to)` 正向替换，为 false 时反向替换。因此源代码写任意一侧的名称都会被替换为正确值，旧版本条件分支中出现的旧类名（或看似未导入的类）不是错误，不要"修复"。建议共享源码统一使用新版本名称（如 `Identifier`、`GuiGraphicsExtractor`）：当前最高版本无需替换，旧版本自动反向替换；两个条件编译块的唯一区别是被替换的类型名时，可以合并为一个块。

### 26.3 起原版窗口后端由 GLFW 换成 SDL（2026-09-22）

Minecraft 26.3 不再依赖 `lwjgl-glfw`，改用 `lwjgl-sdl`：`org.lwjgl.glfw.*` 在 26.3 的编译期和运行期类路径上都不存在，同时原始输入码的含义整体改变（`MOUSE_BUTTON_LEFT` 由 0 变为 1、`MOUSE_BUTTON_RIGHT` 由 1 变为 3、`REPEAT` 由 2 变为 -1）。共享源码不得直接使用任何窗口后端的 API 或常量，应改用原版提供的、与后端无关的入口：时间取 `Blaze3D.getTime()`，鼠标按键与动作码在 bridge 层用 `InputConstants` 的常量翻译成自定义枚举后再交给 logic（见 `MouseInputContext`）。26.3 还把 `Util.getPlatform().openUri` 换成了 `Blaze3D.openUri`，并把渲染缓冲类（如 `GpuBufferSlice`）挪进了 `com.mojang.renderpearl` 包，字符串型 `@At` 目标里的 descriptor 随之改变——这类目标编译期不校验，升级版本后要用 `javap` 对照实际的 named jar 逐个核对。
