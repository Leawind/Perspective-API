package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Represents a pure mathematical modifier that mutates camera states.
///
/// Modifiers are applied sequentially **before** the transition interpolation.
/// They mutate the target state, and the transition will smoothly interpolate from the previous
/// state to this modified target state.
@ApiStatus.OverrideOnly
public interface PerspectiveModifier {

  /// Returns the unique identifier of this perspective.
  ///
  /// Recommended format: `<modid>.<path>`
  ///
  /// Example: `examplemod.free_camera`
  @NonNull String id();

  /// Returns whether this modifier is currently available.
  ///
  /// If `false`, this modifier is skipped during the current frame's camera transformations
  /// but remains registered in the chain for future frames.
  ///
  /// @return `true` to apply, `false` to skip.
  default boolean isAvailable() {
    return true;
  }

  /// Mutates the camera target state in-place.
  ///
  /// @param ctx   The context containing frame-specific data.
  /// @param state The target camera state, potentially modified by the base
  ///   perspective and previous modifiers. Can be mutated.
  default void apply(PerspectiveState.@NonNull Mutable state,@NonNull PerspectiveContext ctx) {}
}
