package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/// Represents a pure mathematical modifier that mutates camera states.
///
/// Modifiers are applied sequentially **before** the transition interpolation.
/// They mutate the target state, and the transition controller will smoothly
/// interpolate from the previous state to this modified target state.
///
public interface PerspectiveModifier {

  /// Returns the unique identifier of this perspective.
  ///
  /// Recommended format: `<modid>:<path>`
  ///
  /// Example: `examplemod:free_camera`
  @NonNull Identifier id();

  /// ```
  /// perspective.<mod_id>.<id>.name
  /// ```
  default @NonNull Component getNameComponent() {
    return Component.translatable(
        "perspective." + id().getNamespace() + "." + id().getPath() + ".name");
  }

  /// ```
  /// perspective.<mod_id>.<id>.description
  /// ```
  default @NonNull Component getDescriptionComponent() {
    return Component.translatable(
        "perspective." + id().getNamespace() + "." + id().getPath() + ".description");
  }

  /// Returns whether this modifier is currently available.
  ///
  /// If `false`, this modifier is skipped during the current frame's camera transformations
  /// but remains registered in the chain for future frames.
  ///
  /// @return `true` to apply, `false` to skip.
  default boolean isAvailable() {
    return true;
  }

  /// Mutates spatial target state in-place.
  ///
  /// @param ctx The context containing frame-specific data.
  /// @param position The target camera position in world space. Can be mutated.
  /// @param rotation The target camera rotation. Can be mutated.
  default void applyTransform(
      @NonNull PerspectiveContext ctx, @NonNull Vector3d position, @NonNull Quaternionf rotation) {}

  /// Calculates the modified Field of View (FOV).
  ///
  /// @param ctx The context containing frame-specific data.
  /// @param currentFovDeg The FOV in degrees, potentially modified by previous modifiers.
  /// @return The new FOV in degrees.
  default float applyFov(@NonNull PerspectiveContext ctx, float currentFovDeg) {
    return currentFovDeg;
  }
}
