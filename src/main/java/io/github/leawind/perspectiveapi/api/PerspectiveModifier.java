package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
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

  /// Returns the translation key for this perspective's display name.
  ///
  /// The default key follows the format: `perspective.<namespace>.<path>`.
  /// For example, an ID of `examplemod:free_camera` produces `perspective.examplemod.free_camera`.
  ///
  /// This key is intended to be used with Minecraft's translation system
  /// to display localized perspective names in GUIs.
  default String translationKey() {
    return "perspective." + id().getNamespace() + "." + id().getPath();
  }

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
