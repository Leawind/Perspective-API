<div align="center">

<img src="src/main/resources/logo.svg" alt="Perspective API" style="image-rendering:pixelated;height:10em;">

# Perspective API

[中文](README.zh.md) | English

![API version](https://img.shields.io/github/v/tag/Leawind/Perspective-API?label=API&color=818181)

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/LIqveQm1?style=flat&logo=modrinth&color=17B85A&cacheSeconds=3600&label=Modrinth)](https://modrinth.com/mod/perspective-api)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1575322?style=flat&logo=curseforge&color=17B85A&cacheSeconds=3600&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/perspective-api)

</div>

Perspective API is a camera perspective management framework for Minecraft client-side mods. It provides standardized interfaces using the JOML library to handle camera states (position, rotation, FOV), decoupled from Minecraft internals.

> [!WARNING]
> This API is currently in **active development preview** and is subject to breaking changes at any time.

## Key Features

- **Roll Support**: Camera rotation is specified with quaternions, fully supporting roll
- **Smooth Transitions**: Built-in interpolated transition system ensures natural and fluid perspective switches for position, rotation, and FOV
- **Priority-Based Override Chain**: A dynamic evaluation mechanism based on priority, where high-priority temporary perspectives (e.g., cutscenes, GUI-forced views) automatically override base perspectives
- **Built-in Perspective Wheel**: Takes over the vanilla F5 toggle key, supporting short-press cycle switching and long-press to open the perspective wheel

## Compatibility Matrix

| Minecraft Version | Fabric | NeoForge | Forge (Legacy) |
| :---------------: | :----: | :------: | :------------: |
|      1.20.1       |   ✅   |    ❌    |       ✅       |
|      1.20.4       |   ✅   |    ✅    |       ❌       |
|      1.20.6       |   ✅   |    ✅    |       ❌       |
|       1.21        |   ✅   |    ✅    |       ❌       |
|      1.21.11      |   ✅   |    ✅    |       ❌       |
|      26.1.x       |   ✅   |    ✅    |       ❌       |
|       26.2        |   ✅   |    ✅    |       ❌       |

## Internal Logic

The camera state is computed during the render tick as follows:

1. **Resolve the current perspective**: Evaluate the override chain from highest to lowest priority. If no high-priority override is active, the current perspective from the perspective wheel is used
2. **Apply the base perspective**: Execute the current `PerspectiveBehavior`'s `applyTransform` and `applyFov` to establish the target camera state
3. **Apply modifiers**: Execute all registered `PerspectiveModifier` instances in ascending priority order, applying additional transformations to the target state
4. **Transition interpolation**: Interpolate between the initial state and the target state
5. **Apply to camera**: Write the final computed result to the Minecraft camera instance

## Built-in Features

### Default Perspectives

The three built-in perspectives correspond one-to-one with the vanilla `CameraType` values:

| Perspective ID                       | Name               |
| ------------------------------------ | ------------------ |
| `perspective_api.first_person`       | First Person       |
| `perspective_api.third_person_back`  | Third Person Back  |
| `perspective_api.third_person_front` | Third Person Front |

### Perspective Wheel

- **Short press F5**: Cycle through available perspectives
- **Long press F5**: Open the radial wheel menu, move the mouse to select a perspective, release to confirm
- **Scroll wheel**: Rotate the option list within the wheel menu

> [!TIP]
> The perspective wheel is the lowest-priority override entry in the override chain. When another mod sets a higher-priority temporary perspective via the override chain, the wheel's selection is temporarily ignored.

## Math Conventions

See the utility class `PerspectiveMath`.

### Euler Angles

The Euler angle convention is consistent with vanilla Minecraft entity and camera code.

| Axis | Meaning | Positive Direction         |
| ---- | ------- | -------------------------- |
| X    | Pitch   | Rotate downward            |
| Y    | Yaw     | Clockwise from above       |
| Z    | Roll    | Clockwise around view axis |

A zero Euler angle $(0, 0, 0)$ corresponds to:

| Direction | Direction Vector |
| --------- | ---------------- |
| Forward   | $(0, 0, 1)$      |
| Up        | $(0, 1, 0)$      |
| Left      | $(-1, 0, 0)$     |

### Quaternions

> [!TIP]
>
> In Minecraft, the camera (`net.minecraft.client.Camera`) computes a quaternion from its Euler angles for rendering. Below 1.21, it uses the +Z axis as the identity quaternion orientation; from 1.21 onward, it uses the -Z axis.
>
> This mod abstracts away that difference and aligns with the zero Euler angle, using +Z as the initial rotation.

The identity quaternion $(w=1, x=0, y=0, z=0)$ represents the same orientation as the zero Euler angle.

Quaternions are constructed from Euler angles using the **Y-X-Z** rotation order:

$$R = R_y(\text{yaw}) \cdot R_x(\text{pitch}) \cdot R_z(\text{roll})$$

## Adding Dependencies

### Modrinth Maven

Notation format: `"maven.modrinth:perspective-api:${version}+${loader}-${minecraft_version}"`

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

## Example Mod

[Perspective API Demo](https://github.com/Leawind/Perspective-API-Demo) implements some custom perspectives using this API and can serve as a development reference.
