package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Represents a pure mathematical modifier that mutates camera states.
///
/// Modifiers are applied sequentially **before** the transition interpolation.
/// They mutate the target state, and the transition will smoothly interpolate from the previous
/// state to this modified target state.
@ApiStatus.OverrideOnly
public interface PerspectiveModifier {

  /// Returns whether this modifier is currently available.
  ///
  /// If `false`, this modifier is skipped during the current frame's camera transformations
  /// but remains registered in the chain for future frames.
  /// A failure is logged and treated as `false` for that frame.
  ///
  /// @return `true` to apply, `false` to skip.
  default boolean isAvailable() {
    return true;
  }

  /// Mutates the camera target state in-place.
  ///
  /// If this method fails, the failure is logged, all changes made by this modifier are reverted,
  /// and later modifiers continue to run.
  ///
  /// @param state The target camera state, potentially modified by the base
  ///   perspective and previous modifiers. Can be mutated.
  /// @param context   The context containing frame-specific data.
  /// @apiNote Both arguments are temporary and must not be retained after this method returns.
  ///   Repeated rotation calculations can accumulate floating-point error, and the camera pipeline
  ///   rejects rotations outside its unit-length tolerance.
  default void apply(
      PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext context) {}
}
