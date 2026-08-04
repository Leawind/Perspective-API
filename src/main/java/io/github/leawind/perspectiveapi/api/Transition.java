package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;

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
}
