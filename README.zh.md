| 中文 | [English](README.md) |
| :--: | :------------------: |

<div align="center">

<img src="src/main/resources/logo.64x.png" alt="Perspective API" style="image-rendering:pixelated;height:10em;">

# 视角API（Perspective API）

![API version](https://img.shields.io/github/v/tag/Leawind/Perspective-API?label=API&color=818181)

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/LIqveQm1?style=flat&logo=modrinth&color=17B85A&cacheSeconds=3600&label=Modrinth)](https://modrinth.com/mod/perspective-api)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1575322?style=flat&logo=curseforge&color=F1643%5E&cacheSeconds=3600&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/perspective-api)

</div>

Perspective API 是一个为 Minecraft 客户端模组设计的相机视角管理框架。它提供一套标准化的接口，使用 JOML 库处理相机的位置、旋转等状态，与 Minecraft 代码解耦。

该框架内置了平滑过渡动画、基于优先级的视角覆盖链以及可配置的视角循环切换机制。

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
|      26.1      |   ✅   |    ✅    |       ❌       |
|      26.2      |   ✅   |    ✅    |       ❌       |

## 开发者指南

> [!WARNING]
> 本API尚不稳定，随时可能发生破坏性变更。

### 添加依赖

#### Modrinth Maven

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
  implementation("maven.modrinth:perspective-api:1.0.0-beta.1+fabric-26.2")
}
```

### 创建自定义视角

实现 `Perspective` 接口以定义新的相机行为。

核心方法说明：

- `getId()`: 返回唯一的 `Identifier`，用于注册和引用。
- `getCameraType()`: 当`applyTransform`和`applyFov`什么也不做时，回退到的原版视角。
- `applyTransform(position, rotation)`: 每帧调用，用于修改相机的位置和朝向。
- `applyFov(fov)`: 每帧调用，用于修改视场角。
- `clientTick()` / `renderTick()`: 分别在客户端逻辑 tick 和渲染 tick 中调用，用于更新内部状态。
- `isAvailable()`: 判断当前视角是否可用。若返回 `false`，覆盖链将跳过此视角。

### 注册视角

你可以实现 `PerspectiveRegistrar` 接口，并通过 Java SPI 机制让本模组在初始化阶段自动发现并注册你的视角。

也可以在你的模组初始化阶段，通过 `PerspectiveAPI.getManager().registry()` 注册。

```java
PerspectiveAPI.getManager().registry().register(MyCustomPerspective.INSTANCE);
```

(可选) 将其加入视角循环列表，使其可通过视角切换键（默认 F5）切换。 `priority` 决定其在循环列表中的顺序

```java
PerspectiveAPI.getManager().cycler().add(MyCustomPerspective.ID, 60);
```

### 管理临时视角覆盖

当需要临时接管相机控制权时（例如：打开特殊 GUI、播放过场动画），请使用覆盖链 (Override Chain)。

覆盖链根据优先级评估每个覆盖项的 `Supplier<Identifier>`。一旦某个覆盖项返回有效的视角 ID，评估即停止并应用该视角。

```java
Identifier id = /* ... */;

PerspectiveAPI.getManager().overrides().push(id, 100, () -> MyGuiPerspective.ID);
```

移除覆盖项以恢复默认行为：

```java
PerspectiveAPI.getManager().overrides().pop(id);
```

### 配置视角循环（可选）

视角循环器 (`PerspectiveCycler`) 管理着玩家通过原版切换键遍历的视角列表。其中内置三种视角，分别对应原版的第一人称、第三人称背面、第三人称正面。

开发者可通过 `manager.cycler().add(id, priority)` 将自定义视角加入循环列表。

循环器本身是一个低优先级的覆盖项，它提供当前循环器中选中的视角 ID。若高优先级覆盖项生效，循环器的选择将被暂时忽略。
