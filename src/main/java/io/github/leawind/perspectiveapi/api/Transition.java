package io.github.leawind.perspectiveapi.api;

import org.jspecify.annotations.NonNull;

/// Controls smooth camera transitions between perspectives.
///
/// When a perspective switch occurs, the camera interpolates from its previous position/rotation
/// to the new perspective's values
public interface Transition {

  /// Sets the transition duration in milliseconds.
  void setDurationMs(double durationMs);

  double getDurationMs();

  /// Sets the blending function used for easing.
  void setBlender(@NonNull Blender blender);

  /// Returns the current blending function.
  @NonNull Blender getBlender();

  /// A blending function that maps a normalized time value `[0, 1]` to an eased output `[0, 1]`.
  @FunctionalInterface
  interface Blender {

    /// Applies the easing function to the given input.
    float blend(float x);
  }
}
