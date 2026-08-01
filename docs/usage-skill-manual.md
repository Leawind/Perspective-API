## Overview

Perspective API is a camera perspective management framework for Minecraft client-side mods. It provides standardized interfaces using the JOML library to handle camera states (position, rotation, FOV), decoupled from Minecraft internals.

## Adding Dependencies

### Via Modrinth Maven

Format: `"maven.modrinth:perspective-api:${version}+${loader}-${minecraft_version}"`

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
  modImplementation("maven.modrinth:LIqveQm1:1.0.0-beta.9+fabric-26.2")
}
```

### Adding `com.google.auto.service` Dependency (Optional)

```kotlin
dependencies {
  compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
  annotationProcessor("com.google.auto.service:auto-service:1.1.1")
}
```

This makes it easier to implement SPI without manually editing service files.

## Runtime Perspective Registration

`PerspectiveInfo.Declaration` is the declarative metadata form used by SPI. For
perspectives created from runtime data, build the corresponding `PerspectiveInfo`
object and retain the returned registration handle:

```java
PerspectiveInfo info =
    PerspectiveInfo.builder(
            "examplemod.preset.550e8400-e29b-41d4-a716-446655440000",
            Component.literal("Combat"))
        .baseType(PerspectiveBehavior.BaseType.THIRD_PERSON_BACK)
        .priority(100)
        .trait("third_person")
        .build();

PerspectiveRegistration registration =
    PerspectiveAPI.getRegistry().register(info, new PresetPerspectiveBehavior(preset));
```

Each registered ID and `PerspectiveBehavior` instance must be unique. Metadata is fixed when the
perspective is registered.

Call `registration.unregister()` to remove only the registration owned by that handle.
An old handle cannot remove a newer registration that reuses the same ID. A default
perspective can be registered with `PerspectiveRegistry.registerDefault`; removing the
last registered default perspective is rejected.

## Perspective Traits

Traits are open, semantic labels that let integrations recognize related perspectives
without hard-coding perspective IDs. Query effective traits through the current
perspective:

```java
boolean isThirdPerson =
    PerspectiveAPI.getCurrent().info().hasTrait("third_person");
```

Shared traits use lowercase `snake_case` without a namespace. Recommended traits are:

- `first_person`: primarily observes from the camera entity's eyes
- `third_person`: primarily observes the camera entity from outside

Trait names form an open vocabulary; perspectives may declare other traits without
registering them first. A mod-specific trait may use the form
`<namespace>:<trait>`, such as `examplemod:cinematic_tracking`, until a shared
meaning is established.

Traits describe stable properties of a perspective, not transient per-frame state. For
example, use `third_person` to decide whether entity fading is applicable, but calculate
the current camera-to-entity distance from the actual camera state. Do not infer traits
from `baseType`: it is only the vanilla fallback camera type, and a free camera may also
use `THIRD_PERSON_BACK` as its fallback.

Traits are specified by the perspective provider when the perspective is registered and
cannot be changed afterward. Other mods cannot add traits to or remove traits from a
perspective. Use `perspective.info().traits()` to inspect all declared traits, or
`perspective.info().hasTrait(...)` for a single behavior check.

## References

- Mod source code: https://github.com/Leawind/Perspective-API
- [Perspective API Demo](https://github.com/Leawind/Perspective-API-Demo) implements custom perspectives and can serve as a development reference.

## Camera State Calculation Process

Camera state calculation is divided into two phases: Client Tick and Render Tick.

### Client Tick

Triggered at the `CLIENT_TICK_START` event, executed once per game tick:

1. Execute switcher callback: call `PerspectiveSwitcherBehavior#clientTickWhenActive`
2. Resolve current perspective: obtain the perspective ID from the override chain, resolve it to a `Perspective` object via the registry
3. Perspective switch handling: if the current perspective changes, trigger `onDeactivate`/`onActivate`, start transition animation
4. Execute perspective callback: call `PerspectiveBehavior#clientTickWhenActive`

### Render Tick

Triggered at the `SETUP_CAMERA` event, executed per render frame:

1. Pre-apply callback: execute `PerspectiveBehavior.preApplyWhenActive`
2. Apply base perspective: execute `PerspectiveBehavior.applyCameraState`
3. Apply modifier chain: execute all `PerspectiveModifier.apply`
4. Transition interpolation: if transitioning, interpolate camera states
5. Write to camera: write the final result to the Minecraft camera instance
6. Post-apply callback: execute `PerspectiveBehavior.postApplyWhenActive` with the final read-only camera state

## Mathematical Conventions

### Euler Angles

Consistent with vanilla Minecraft:

| Axis | Meaning | Positive Direction          |
| ---- | ------- | --------------------------- |
| X    | Pitch   | Rotating downward           |
| Y    | Yaw     | Clockwise from above        |
| Z    | Roll    | Clockwise around sight axis |

Zero Euler angles (0, 0, 0) correspond to facing south.

### Quaternions

The identity quaternion (w=1, x=0, y=0, z=0) represents the same orientation as zero Euler angles. Quaternions are constructed from Euler angles using Y-X-Z rotation order: R = Ry(yaw) * Rx(pitch) * Rz(roll).

All quaternions used are unit quaternions. The API uses JOML types (`Quaternionf`, `Quaternionfc`).

### Angle Parameter Naming

Angle parameters and variables use suffixes to indicate units:

- `Deg` suffix for degrees (e.g., `vanillaFovDeg`, `pitchDeg`)
- `Rad` suffix for radians (e.g., `pitchRad`, `yawRad`)

## Built-in Default Perspectives

- `perspective_api.first_person`
- `perspective_api.third_person_back`
- `perspective_api.third_person_front`
