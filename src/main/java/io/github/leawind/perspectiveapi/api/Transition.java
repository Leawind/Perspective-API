package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;

/// Controls smooth camera transitions between perspectives.
///
/// When a perspective switch occurs, the camera interpolates from its previous position/rotation
/// to the new perspective's values
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface Transition {

  /// Sets the transition duration in milliseconds.
  void setDurationMs(double durationMs);

  double getDurationMs();

  /// Sets the blend power exponent applied to the eased progress.
  ///
  /// A value of `1` means no modification; values less than `1` accelerate the start of the
  /// transition; values greater than `1` delay the start.
  void setBlendPower(double blendPower);

  double getBlendPower();
}
