| [中文](README.zh.md) | English |
| :------------------: | :-----: |

<div align="center">

<img src="src/main/resources/logo.png" alt="Perspective API" style="image-rendering:pixelated;height:6em;">

<span style="font-size:0.7em;color:#888">(no logo yet)</span>

# Perspective API

![API version](https://img.shields.io/github/v/tag/Leawind/Perspective-API?label=API&color=818181)

[![Modrinth Downloads](https://img.shields.io/modrinth/dt/LIqveQm1?style=flat&logo=modrinth&color=17B85A&cacheSeconds=3600&label=Modrinth)](https://modrinth.com/mod/perspective-api)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1575322?style=flat&logo=curseforge&color=F1643%5E&cacheSeconds=3600&label=CurseForge)](https://www.curseforge.com/minecraft/mc-mods/perspective-api)

</div>

Perspective API is a camera perspective management framework designed for Minecraft client-side mods. It provides a standardized set of interfaces that leverage the JOML library to handle camera states such as position and rotation, while remaining decoupled from Minecraft's underlying code.

The framework includes built-in smooth transition animations, a priority-based perspective override chain, and a configurable perspective cycling mechanism.

## Key Features

- **Roll**: You can specify camera rotation with quaternion, roll is supported
- **Smooth Transitions**: Supports interpolated transitions for camera position, rotation, and Field of View (FOV), ensuring natural and fluid perspective switches
- **Priority-Based Override Chain**: Introduces a dynamic evaluation mechanism based on priority. High-priority temporary perspectives (e.g., cutscenes, GUI-forced views) automatically override base perspectives
- **Built-in Perspective Cycler**: Takes over the vanilla perspective toggle key (F5), allowing players to cycle through registered perspectives

## Compatibility Matrix

| Minecraft Version | Fabric | NeoForge | Forge (Legacy) |
| :---------------: | :----: | :------: | :------------: |
|      1.20.1       |   ✅   |    ❌    |       ✅       |
|      1.20.4       |   ✅   |    ✅    |       ❌       |
|      1.20.6       |   ✅   |    ✅    |       ❌       |
|       1.21        |   ✅   |    ✅    |       ❌       |
|      1.21.11      |   ✅   |    ✅    |       ❌       |
|       26.1        |   ✅   |    ✅    |       ❌       |
|       26.2        |   ✅   |    ✅    |       ❌       |

## Developer Guide

> [!WARNING]
> This API is currently unstable and subject to breaking changes at any time.

### Adding Dependencies

#### Modrinth Maven

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
  implementation("maven.modrinth:perspective-api:1.0.0-beta.1+fabric-26.2")
}
```

### Creating Custom Perspectives

Implement the `Perspective` interface to define new camera behaviors.

**Core Methods:**

- `getId()`: Returns a unique `Identifier` for registration and reference.
- `getCameraType()`: Specifies the vanilla camera type to fall back to when `applyTransform` and `applyFov` perform no modifications.
- `applyTransform(position, rotation)`: Called every frame to modify the camera's position and orientation.
- `applyFov(fov)`: Called every frame to modify the Field of View.
- `clientTick()` / `renderTick()`: Called during client logic ticks and render ticks, respectively, to update internal state.
- `isAvailable()`: Determines if the current perspective is available. If `false`, the override chain will skip this perspective.

### Registering Perspectives

You can implement the `PerspectiveRegistrar` interface and use the Java SPI mechanism to allow the mod to automatically discover and register your perspectives during initialization.

Alternatively, you can register manually during your mod's initialization phase via `PerspectiveAPI.getManager().registry()`.

```java
PerspectiveAPI.getManager().registry().register(MyCustomPerspective.INSTANCE);
```

(Optional) Add it to the perspective cycle list to make it switchable via the perspective toggle key (default F5). The `priority` determines its order in the cycle.

```java
PerspectiveAPI.getManager().cycler().add(MyCustomPerspective.ID, 60);
```

### Managing Temporary Perspective Overrides

Use the **Override Chain** when you need to temporarily take control of the camera (e.g., when opening a custom GUI or playing a cutscene).

The override chain evaluates the `Supplier<Identifier>` of each entry based on priority. Once an entry returns a valid perspective ID, evaluation stops, and that perspective is applied.

```java
Identifier id = /* ... */;

PerspectiveAPI.getManager().overrides().push(id, 100, () -> MyGuiPerspective.ID);
```

Remove the override entry to restore default behavior:

```java
PerspectiveAPI.getManager().overrides().pop(id);
```

### Configuring Perspective Cycling (Optional)

The `PerspectiveCycler` manages the list of perspectives traversed by the vanilla toggle key. It includes three built-in perspectives corresponding to vanilla First-person, Third-person Back, and Third-person Front.

Developers can add custom perspectives to the cycle list using `manager.cycler().add(id, priority)`.

The cycler itself acts as a low-priority override entry, providing the ID of the currently selected perspective in the cycle. If a higher-priority override is active, the cycler's selection is temporarily ignored.
