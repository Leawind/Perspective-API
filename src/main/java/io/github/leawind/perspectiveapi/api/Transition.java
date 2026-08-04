package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Controls smooth camera transitions between perspectives.
///
/// When a perspective switch occurs, the camera interpolates its position, rotation, FOV, and
/// orthographic height toward the new perspective's values. Projection mode changes are discrete.
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface Transition {

  /// Sets the transition duration in milliseconds.
  ///
  /// A duration of `0` disables interpolation.
  ///
  /// @throws IllegalArgumentException if `durationMs` is negative or not finite
  void setDurationMs(double durationMs);

  double getDurationMs();

  /// Sets the easing function shared by every transition algorithm.
  ///
  /// The function receives normalized elapsed time in `[0, 1]`. Its result is clamped to `[0, 1]`
  /// before algorithms use it; a non-finite result is treated as `0`.
  void setBlender(@NonNull Blender blender);

  /// Returns the easing function shared by every transition algorithm.
  @NonNull Blender getBlender();

  /// Maps normalized elapsed time to normalized transition progress.
  @FunctionalInterface
  interface Blender {

    /// Applies this easing function to normalized `progress` in `[0, 1]`.
    float blend(float progress);
  }
}
