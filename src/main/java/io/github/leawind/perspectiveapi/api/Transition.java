package io.github.leawind.perspectiveapi.api;

import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

/// Controls smooth camera transitions between perspectives.
///
/// When a perspective switch occurs, the camera interpolates from its previous position/rotation
/// to the new perspective's values
public interface Transition extends TransitionController {

  /// Starts a new transition from the given start state.
  ///
  /// @param startTime current timestamp in milliseconds
  /// @param state the start state to transition from
  void setStartState(
      double startTime, Vector3dc startPosition, Quaternionfc startRotation, float startFov);

  void updateTransform(double now, Vector3dc targetPosition, Quaternionfc targetRotation);
  
  float updateFov(double now, float targetFov);

  @NonNull Vector3dc getCurrentPosition();

  @NonNull Quaternionfc getCurrentRotation();

  float getCurrentFov();
}
