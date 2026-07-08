<div align="center">

<img src="src/main/resources/logo.svg" alt="Perspective API" style="image-rendering:pixelated;height:10em;">

# 视角API（Perspective API）

中文 | [English](README.md)

![API version](https://img.shields.io/github/v/tag/Leawind/Perspective-API?label=API&color=818181)

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/LIqveQm1?style=flat&logo=modrinth&color=17B85A&cacheSeconds=3600&label=Modrinth)](https://modrinth.com/mod/perspective-api)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1575322?style=flat&logo=curseforge&color=F1643%5E&cacheSeconds=3600&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/perspective-api)

</div>

Perspective API 是一个为 Minecraft 客户端模组设计的相机视角管理框架。它提供一套标准化的接口，使用 JOML 库处理相机的位置、旋转等状态，与 Minecraft 代码解耦。

## 核心特性

- **滚转角**：可以通过四元数指定相机的旋转，支持滚转角
- **平滑过渡**：支持相机位置、旋转角度及视场角 (FOV) 的插值过渡，确保视角切换自然流畅
- **优先级覆盖链 (Override Chain)**：引入基于优先级的动态评估机制。高优先级的临时视角（如过场动画、GUI 强制视角）会自动覆盖基础视角
- **内置视角循环器**：接管原版视角切换按键（F5），允许玩家在注册的视角列表中循环切换

## 兼容性矩阵

| Minecraft 版本 | Fabric | NeoForge | Forge (Legacy) |
| :------------: | :----: | :------: | :------------: |
|     1.20.1     |   ✅   |    ❌    |       ✅       |
|     1.20.4     |   ✅   |    ✅    |       ❌       |
|     1.20.6     |   ✅   |    ✅    |       ❌       |
|      1.21      |   ✅   |    ✅    |       ❌       |
|    1.21.11     |   ✅   |    ✅    |       ❌       |
|     26.1.x     |   ✅   |    ✅    |       ❌       |
|      26.2      |   ✅   |    ✅    |       ❌       |

> [!WARNING]
> 本API尚不稳定，随时可能发生破坏性变更。

## 核心概念

### 视角（Perspective）

`Perspective` 定义了一种相机行为。每个视角拥有唯一的 `Identifier`，并通过逐帧回调修改相机的位置、旋转和视场角。它还提供生命周期钩子（`onActivate` / `onDeactivate`）和可用性检查（`isAvailable`）。

当 `applyTransform` 和 `applyFov` 未做任何修改时，相机会回退到由 `cameraType()` 指定的原版视角类型。

切换到该视角或从该视角切换出时的平滑过渡可以独立启用或禁用。

### 视角注册

可以通过 Java SPI 机制（`PerspectiveRegistrar` 接口）自动发现并注册视角，也可以通过注册表手动注册。

### 覆盖链（Override Chain）

覆盖链是一种基于优先级的临时相机控制评估机制。每个覆盖项提供一个 `Supplier<Identifier>`，按优先级从高到低依次评估。第一个返回有效视角 ID 的覆盖项胜出，其视角被应用。适用于需要临时接管相机的场景，如自定义 GUI 或过场动画。

### 视角循环器（Perspective Cycler）

循环器管理玩家通过原版切换键（F5）遍历的视角列表。内置三种视角，分别对应原版的第一人称、第三人称背面和第三人称正面。开发者可以添加自定义视角，通过优先级决定其在循环列表中的位置。

循环器本身是一个低优先级的覆盖项。若高优先级覆盖项生效，循环器的选择将被暂时忽略。

### 视角修饰器（Perspective Modifier）

修饰器在基础视角建立目标状态**之后**、过渡插值**之前**，对相机状态施加额外的数学变换。适用于屏幕震动、移动倾斜等需要叠加在任意视角之上的效果。

修饰器通过 key 注册，按优先级升序依次执行。

**执行顺序：** 基础视角 → 修饰器（按优先级） → 净化 → 过渡

## 添加依赖

### Modrinth Maven

格式： `"maven.modrinth:perspective-api:${version}+${loader}-${minecraft_version}"`

```build.gradle.kts
repositories {
  exclusiveContent {
    forRepository {
      maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
      }
    }
    filter {
      includeGroup("maven.modrinth")
    }
  }
}

dependencies {
  // Use `implementation` for >=26.1
  modImplementation("maven.modrinth:LIqveQm1:1.0.0-beta.8+fabric-26.2")
}
```

## 示例模组

模组 [视角API演示](https://github.com/Leawind/Perspective-API-Demo) 利用本 API 实现了一些简单而有趣的功能，可以参考其源码。
