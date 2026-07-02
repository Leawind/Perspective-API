package io.github.leawind.perspectiveapi.api;

import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

/// Controls smooth camera transitions between perspectives.
///
/// When a perspective switch occurs, the camera interpolates from its previous position/rotation
/// to the new perspective's values.
public interface Transition extends TransitionController {

  /// Starts a new transition from the given start state.
  ///
  /// @param startTimeMs Current timestamp in milliseconds.
  /// @param startPosition The start position to transition from.
  /// @param startRotation The start rotation to transition from.
  /// @param startFov The start FOV to transition from.
  void setStartState(
      double startTimeMs, Vector3dc startPosition, Quaternionfc startRotation, float startFov);

  /// Updates the current interpolated position and rotation based on the target state.
  ///
  /// @param currentTimeMs Current timestamp in milliseconds.
  /// @param targetPosition The target position to interpolate towards.
  /// @param targetRotation The target rotation to interpolate towards.
  void updateTransform(double currentTimeMs, Vector3dc targetPosition, Quaternionfc targetRotation);

  /// Updates and returns the current interpolated FOV based on the target FOV.
  ///
  /// @param currentTimeMs       Current timestamp in milliseconds.
  /// @param targetFov The target FOV to interpolate towards.
  /// @return The interpolated FOV.
  float updateFov(double currentTimeMs, float targetFov);

  /// Returns the current interpolated camera position.
  @NonNull Vector3dc getCurrentPosition();

  /// Returns the current interpolated camera rotation.
  @NonNull Quaternionfc getCurrentRotation();

  /// Returns the current interpolated FOV.
  float getCurrentFov();
}
