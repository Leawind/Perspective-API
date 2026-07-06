package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/// Manages an ordered chain of {@link PerspectiveModifier}s.
///
/// Modifiers are applied sequentially by ascending priority after the base perspective
/// establishes the target camera state, but before the transition interpolation.
public interface PerspectiveModifierChain {

  /// Registers a modifier with the given priority.
  ///
  /// If an entry with the same key already exists, it is replaced.
  /// Lower priority values are applied first.
  void register(@NonNull Identifier key, int priority, @NonNull PerspectiveModifier modifier);

  /// Removes the modifier entry with the given key.
  void unregister(@NonNull Identifier key);

  /// Applies all active modifiers' spatial transformations sequentially.
  ///
  /// Called after the base perspective establishes the target state,
  /// but before transition interpolation.
  void applyTransform(
      @NonNull PerspectiveContext ctx, @NonNull Vector3d position, @NonNull Quaternionf rotation);

  /// Applies all active modifiers' FOV calculations sequentially.
  ///
  /// @param ctx The context containing frame-specific data.
  /// @param fov The current FOV in degrees, potentially modified by the base perspective.
  /// @return The final FOV after all modifiers, in degrees.
  float applyFov(@NonNull PerspectiveContext ctx, float fov);
}
