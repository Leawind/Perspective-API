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

  /// Sets the blend power exponent applied to the eased progress.
  ///
  /// A value of `1` means no modification; values less than `1` accelerate the start of the
  /// transition; values greater than `1` delay the start.
  ///
  /// @throws IllegalArgumentException if `blendPower` is not finite or is not greater than `0`
  void setBlendPower(double blendPower);

  double getBlendPower();
}
