package io.github.leawind.perspectiveapi.internal.impl.transition;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveStateImpl;
import io.github.leawind.perspectiveapi.internal.utils.Utils;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/// Feeds target translation and world-space rotation changes directly into the camera while
/// correcting the remaining difference within the fixed transition duration.
///
/// The correction fraction for each update is `deltaTime / previousRemainingTime`. Rotation uses
/// slerp for the same fraction, which is the quaternion exponential map of the shortest-path error.
/// FOV and orthographic height use fixed-start linear interpolation so valid endpoint values cannot
/// produce an invalid intermediate projection value. The first update establishes the initial
/// target sample; target motion is forwarded from the following update onward.
public final class FeedForwardCorrectionTransitionAlgorithm implements TransitionAlgorithm {

  private final Vector3d currentPosition = new Vector3d();
  private final Quaternionf currentRotation = new Quaternionf();
  private float startFovDeg = PerspectiveStateImpl.DEFAULT_FOV_DEGREES;
  private float startOrthographicHeight = PerspectiveStateImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT;

  private boolean hasPreviousTarget;
  private double previousElapsedTimeMs;
  private final Vector3d previousTargetPosition = new Vector3d();
  private final Quaternionf previousTargetRotation = new Quaternionf();

  private final Vector3d targetPosition = new Vector3d();
  private final Quaternionf targetRotation = new Quaternionf();
  private final Quaternionf inversePreviousTargetRotation = new Quaternionf();
  private final Quaternionf targetRotationDelta = new Quaternionf();
  private final Quaternionf feedForwardRotation = new Quaternionf();

  @Override
  public void start(@NonNull PerspectiveState startState) {
    Objects.requireNonNull(startState);
    currentPosition.set(startState.position());
    currentRotation.set(startState.rotation());
    startFovDeg = startState.getFovDeg();
    startOrthographicHeight = startState.getOrthographicHeight();
    hasPreviousTarget = false;
    previousElapsedTimeMs = 0;
  }

  @Override
  public void update(
      double elapsedTimeMs,
      double durationMs,
      @NonNull PerspectiveState target,
      PerspectiveState.@NonNull Mutable dest) {
    Objects.requireNonNull(target);
    Objects.requireNonNull(dest);

    targetPosition.set(target.position());
    targetRotation.set(target.rotation());
    float targetFovDeg = target.getFovDeg();
    float targetOrthographicHeight = target.getOrthographicHeight();

    double currentElapsedTimeMs = Utils.clamp(elapsedTimeMs, previousElapsedTimeMs, durationMs);
    double deltaTimeMs = currentElapsedTimeMs - previousElapsedTimeMs;
    double previousRemainingTimeMs = durationMs - previousElapsedTimeMs;
    float correctionFraction =
        previousRemainingTimeMs <= deltaTimeMs
            ? 1
            : (float) (deltaTimeMs / previousRemainingTimeMs);

    alignTargetRotationSign();
    if (hasPreviousTarget) {
      currentPosition.add(targetPosition).sub(previousTargetPosition);

      previousTargetRotation.invert(inversePreviousTargetRotation);
      targetRotation.mul(inversePreviousTargetRotation, targetRotationDelta).normalize();
      targetRotationDelta.mul(currentRotation, feedForwardRotation).normalize();
      currentRotation.set(feedForwardRotation);
    }

    currentPosition.lerp(targetPosition, correctionFraction);
    currentRotation.slerp(targetRotation, correctionFraction).normalize();

    float progress = (float) (currentElapsedTimeMs / durationMs);
    dest.position().set(currentPosition);
    dest.rotation().set(currentRotation);
    dest.setFovDeg(lerp(startFovDeg, targetFovDeg, progress));
    dest.setOrthographicHeight(
        lerp(startOrthographicHeight, targetOrthographicHeight, progress));

    previousTargetPosition.set(targetPosition);
    previousTargetRotation.set(targetRotation);
    previousElapsedTimeMs = currentElapsedTimeMs;
    hasPreviousTarget = true;
  }

  private void alignTargetRotationSign() {
    Quaternionf reference = hasPreviousTarget ? previousTargetRotation : currentRotation;
    if (reference.dot(targetRotation) < 0) {
      targetRotation.mul(-1);
    }
  }

  private static float lerp(float start, float target, float progress) {
    return start + (target - start) * progress;
  }
}
