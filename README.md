<div align="center">

<img src="src/main/resources/logo.svg" alt="Perspective API" style="image-rendering:pixelated;height:10em;">

# Perspective API

[中文](README.zh.md) | English

![API version](https://img.shields.io/github/v/tag/Leawind/Perspective-API?label=API&color=818181)

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/LIqveQm1?style=flat&logo=modrinth&color=17B85A&cacheSeconds=3600&label=Modrinth)](https://modrinth.com/mod/perspective-api)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1575322?style=flat&logo=curseforge&color=F1643%5E&cacheSeconds=3600&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/perspective-api)

</div>

Perspective API is a camera perspective management framework for Minecraft client-side mods. It provides standardized interfaces using the JOML library to handle camera states (position, rotation, FOV), decoupled from Minecraft internals.

## Key Features

- **Roll**: Camera rotation is specified with quaternions, so roll is fully supported
- **Smooth Transitions**: Interpolated transitions for position, rotation, and FOV ensure natural perspective switches
- **Priority-Based Override Chain**: High-priority temporary perspectives (e.g., cutscenes, GUI-forced views) automatically override base perspectives
- **Built-in Perspective Wheel**: Takes over the vanilla F5 toggle key

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

> [!WARNING]
> This API is currently unstable and subject to breaking changes at any time.

## Core Concepts

### Perspective

A `Perspective` defines a camera behavior. Each perspective has a unique `Identifier` and implements per-frame callbacks to modify camera position, rotation, and FOV. It also exposes lifecycle hooks (`onActivate` / `onDeactivate`) and availability checks (`isAvailable`).

When `applyTransform` and `applyFov` make no modifications, the camera falls back to a vanilla camera type specified by `cameraType()`.

Transitions in and out of a perspective can be individually enabled or disabled.

### Perspective Registration

Perspectives can be registered via the Java SPI mechanism (`PerspectiveRegistrar` interface) for automatic discovery, or manually through the registry.

### Override Chain

The override chain is a priority-based evaluation mechanism for temporary camera control. Each entry provides a `Supplier<Identifier>` evaluated by descending priority. The first entry to return a valid perspective ID wins, and that perspective is applied. This is ideal for scenarios like custom GUIs or cutscenes that need to temporarily take over the camera.

### Perspective Wheel

The wheel manages the list of perspectives players traverse with the vanilla toggle key (F5). It includes three built-in perspectives corresponding to vanilla First-person, Third-person Back, and Third-person Front. Custom perspectives can be registered with a priority that determines their position in the cycle.

The wheel itself acts as a low-priority override entry. If a higher-priority override is active, the wheel's selection is temporarily ignored.

### Perspective Modifier

Modifiers apply additional mathematical transformations to the camera state **after** the base perspective establishes the target state but **before** transition interpolation. This is useful for effects like screen shake, movement tilt, or vehicle roll that should layer on top of any perspective.

Modifiers are registered with a key, a priority, and executed in ascending priority order.

## Perspective State Processing Order

1. Calculate the base perspective via the override chain
   - If no other override is active, the current perspective from the built-in Perspective Wheel is used, as it is the lowest-priority override in the chain
2. Apply modifiers in priority order
   - No modifiers are registered by default
3. Handle camera state transitions
   - Transition duration is a fixed value and can be customized
4. Apply the state to the camera

## Adding Dependencies

### Modrinth Maven

Notation format: `"maven.modrinth:perspective-api:${version}+${loader}-${minecraft_version}"`

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

## Example Mod

The mod [Perspective API Demo](https://github.com/Leawind/Perspective-API-Demo) uses this API to implement some simple yet interesting features. You can refer to its source code for guidance.
