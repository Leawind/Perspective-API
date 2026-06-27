| [中文](README.zh.md) | English |
| -------------------- | ------- |

# Perspective API

> [!WARNING]
> This API is currently unstable and breaking changes may occur at any time in the future.

Perspective API is a client-side API mod designed to take over and extend Minecraft's vanilla camera system. It intercepts and controls the camera's position, rotation (including roll), and field of view (FOV) via Mixins, providing a powerful and flexible custom perspective management framework for other mods.

This mod itself does not provide additional in-game perspectives (except for built-in vanilla perspective replacements), but rather serves as a foundational dependency for other mods to use.

## Core Architecture

All features are coordinated through a global singleton `PerspectiveManager` (obtained via `PerspectiveAPI.getManager()`), which contains four core components:

- **Registry**: A global singleton responsible for storing and managing all registered `Perspective` instances.
- **Override Chain**: A priority-based perspective resolution mechanism that allows mods to temporarily take over the perspective under specific conditions (e.g., riding a vehicle, aiming).
- **Cycler**: Manages the list of perspectives that players cycle through via a keybind (default F5).
- **Transition Controller**: Handles smooth interpolation animations when switching between perspectives.

## Custom Perspectives

Developers can define entirely new camera behaviors by implementing the `Perspective` interface. A custom perspective primarily consists of the following macro-level logic:

### State Control

A perspective can provide the desired camera state (world-space position, rotation quaternion, FOV) on every frame. These states are **optional**; any unprovided attributes will automatically fall back to the vanilla camera logic dictated by the `CameraType` (for example, the built-in first-person perspective provides no state at all, completely retaining vanilla behavior).

### Lifecycle and Events

`Perspective` provides comprehensive lifecycle callbacks, allowing developers to execute custom logic when a perspective is activated/deactivated, on client ticks, and on render ticks.

### Registration Mechanism

Custom perspectives are typically registered during the mod initialization phase. It is recommended to implement the `PerspectiveRegistrar` interface via the Java SPI mechanism; this mod will automatically discover and invoke it during the loading phase. Alternatively, you can manually register them by obtaining the registry instance directly through `PerspectiveManager#registry()`.

## Override Chain and Perspective Resolution

The `PerspectiveManager` determines the actual perspective rendered on each frame via the **Override Chain**. The override chain is a priority-based list of entries, where each entry contains a unique identifier (Key), a priority (Priority), and a `Supplier` that provides a perspective ID.

### Resolution Process

On every client tick, the system traverses the override chain in **descending order of priority**:

1. Calls the `Supplier` of the current entry to obtain the `Identifier` of the target perspective.
2. If the ID is non-null and registered in the registry, that perspective is immediately adopted and the resolution ends.
3. If the ID is null or invalid, the system continues to evaluate the next entry with a lower priority.
4. If the entire override chain is traversed without hitting a valid perspective, it falls back to the system's hardcoded default perspective (the built-in first-person perspective).

### Cycler

The functionality for players to switch perspectives via a keybind (default F5) is implemented by the `PerspectiveCycler`. The Cycler itself is a built-in entry in the override chain, with its priority set to `Integer.MIN_VALUE` (the lowest priority).

This means:

- High-priority override entries pushed by other mods (e.g., riding a vehicle, using a scope) will temporarily "override" the player's manual selection.
- When all high-priority temporary override conditions are not met (the `Supplier` returns `null`), the resolution process naturally flows to the Cycler entry, thereby applying the perspective currently selected by the player.

## Smooth Transitions

When the resolved "current perspective" changes, the `Transition` component captures the camera state at the moment of the switch and smoothly interpolates the camera to the target state of the new perspective over a configured duration, using a customizable easing function (Blender). Perspectives can also choose to disable this transition effect via their configuration.

## Compatibility Matrix

This mod contains client-side logic only. The project is built on Stonecutter and supports the following versions and platforms:

| Minecraft Version | Fabric | Forge | NeoForge |
| :---------------- | :----: | :---: | :------: |
| **1.20.1**        |   ✅   |  ✅   |    ❌    |
| **1.20.4**        |   ✅   |  ❌   |    ✅    |
| **1.20.6**        |   ✅   |  ❌   |    ✅    |
| **1.21**          |   ✅   |  ❌   |    ✅    |
| **1.21.11**       |   ✅   |  ❌   |    ✅    |
| **26.1**          |   ✅   |  ❌   |    ✅    |
| **26.1.2**        |   ✅   |  ❌   |    ✅    |
| **26.2**          |   ✅   |  ❌   |    ✅    |
