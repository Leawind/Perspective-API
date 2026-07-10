package io.github.leawind.perspectiveapi.api;

/// Controls smooth camera transitions between perspectives.
///
/// When a perspective switch occurs, the camera interpolates from its previous position/rotation
/// to the new perspective's values
public interface Transition {

  /// Sets the transition duration in milliseconds.
  void setDurationMs(double durationMs);

  double getDurationMs();
}
