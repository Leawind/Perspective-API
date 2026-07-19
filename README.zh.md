<div align="center">

<img src="src/main/resources/logo.svg" alt="Perspective API" style="image-rendering:pixelated;height:10em;">

# 视角 API（Perspective API）

中文 | [English](README.md)

![API version](https://img.shields.io/github/v/tag/Leawind/Perspective-API?label=API&color=818181)

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/LIqveQm1?style=flat&logo=modrinth&color=17B85A&cacheSeconds=3600&label=Modrinth)](https://modrinth.com/mod/perspective-api)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1575322?style=flat&logo=curseforge&cacheSeconds=3600&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/perspective-api)

</div>

Perspective API 是一个为 Minecraft 客户端模组设计的相机视角管理框架。它提供一套标准化的接口，使用 JOML 库处理相机的位置、旋转等状态，与 Minecraft 原版代码解耦。

> [!WARNING]
> 本项目目前处于**开发预览阶段**，API 可能随时发生破坏性变更。

## 核心特性

- **滚转角支持**：通过四元数指定相机旋转，支持滚转角（Roll）
- **平滑过渡动画**：内置插值过渡系统，确保视角切换时位置、旋转和视场角 (FOV) 自然流畅
- **优先级覆盖链**：基于优先级的动态评估机制，高优先级临时视角（如过场动画、GUI 强制视角）可自动覆盖基础视角
- **内置轮盘切换器**：接管原版 F5 按键逻辑，支持短按循环切换与长按打开视角轮盘

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

## 内部逻辑

本模组在 render tick 中计算相机状态的逻辑如下：

1. **解析当前视角**：通过覆盖链（Override Chain）按优先级从高到低评估，若无高优先级覆盖项生效，将使用轮盘切换器提供的当前视角
2. **应用基础视角**：执行当前 `PerspectiveBehavior` 的 `applyTransform` 和 `applyFov`，建立目标相机状态
3. **叠加修饰器**：按优先级升序依次执行所有已注册的 `PerspectiveModifier`，对目标状态施加额外变换
4. **过渡插值**：在起始状态与目标状态之间进行插值
5. **应用到相机**：将最终计算结果写入 Minecraft 相机实例

## 内置功能

### 默认视角

内置的三个视角与原版的三种 `CameraType` 一一对应：

| 视角 ID                              | 名称         |
| ------------------------------------ | ------------ |
| `perspective_api.first_person`       | 第一人称     |
| `perspective_api.third_person_back`  | 第三人称背面 |
| `perspective_api.third_person_front` | 第三人称正面 |

### 视角轮盘

- **短按 F5**：在可用视角列表中循环切换
- **长按 F5**：打开径向轮盘菜单，移动鼠标选择视角，松开确认
- **滚轮**：在轮盘菜单中旋转选项列表

> [!TIP]
> 轮盘切换器是覆盖链中优先级最低的覆盖项。当其他模组通过覆盖链设置了更高优先级的临时视角时，轮盘的选择将被暂时忽略。

## 数学约定

参考工具类 `PerspectiveMath`。

### 欧拉角

欧拉角约定与 Minecraft 原版实体及相机代码完全一致。

| 维度 | 含义  | 正方向           |
| ---- | ----- | ---------------- |
| X    | Pitch | 向下旋转         |
| Y    | Yaw   | 从上往下看顺时针 |
| Z    | Roll  | 绕视线轴顺时针   |

零欧拉角 $(0, 0, 0)$ 对应的朝向：

| 方向 | 方向向量     |
| ---- | ------------ |
| 前方 | $(0, 0, 1)$  |
| 上方 | $(0, 1, 0)$  |
| 左方 | $(-1, 0, 0)$ |

### 四元数

> [!TIP] 提示
>
> 在 Minecraft 中，相机 `net.minecraft.client.Camera` 会根据自身的欧拉角计算对应的四元数用于渲染，在 1.21 以下，它以 Z 轴正向作为恒等四元数的朝向，在 1.21 及以上则是 Z 轴负向。
>
> 本模组屏蔽了这一差异，与零欧拉角对齐，采用 +Z 作为初始旋转。

恒等四元数 $(w=1, x=0, y=0, z=0)$ 与零欧拉角表示相同的朝向。

从欧拉角构造四元数时采用 **Y-X-Z** 旋转顺序：

$$R = R_y(\text{yaw}) \cdot R_x(\text{pitch}) \cdot R_z(\text{roll})$$

## 添加依赖

### Modrinth Maven

格式：`"maven.modrinth:perspective-api:${version}+${loader}-${minecraft_version}"`

```kotlin
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

[视角 API 演示](https://github.com/Leawind/Perspective-API-Demo) 利用本 API 实现了一些自定义视角，可作为开发参考。
