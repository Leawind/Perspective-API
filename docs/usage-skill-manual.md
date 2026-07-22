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

1. Setup `PerspectiveContext` (including `partialTicks`, `entity`, `isTransitioning`)
2. Execute render callback: call `PerspectiveBehavior.renderTickWhenActive`
3. Backup vanilla state: save vanilla camera position and rotation
4. Apply base perspective: execute `PerspectiveBehavior.applyTransform` to modify position and rotation
5. Apply modifier chain: execute all `PerspectiveModifier.applyTransform` in ascending priority order
6. Validate state: check validity of position and rotation, fallback to vanilla state if invalid
7. Apply transition interpolation: if transitioning, interpolate between the previous frame's state and the target state
8. Write to camera: write the final result to the Minecraft camera instance

FOV modification is handled separately in the `MODIFY_FIELD_OF_VIEW` event, with a similar process: base perspective `applyFov` → modifier chain → output.

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
