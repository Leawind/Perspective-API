package io.github.leawind.perspectiveapi.internal.impl.transition;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveStateImpl;
import io.github.leawind.perspectiveapi.internal.utils.Utils;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blender;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blenders;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/// Feeds target translation and world-space rotation changes directly into the camera while
/// correcting the remaining difference within the fixed transition duration.
///
/// Each update derives its correction fraction from the change in eased progress divided by the
/// previous remaining eased progress. Rotation uses slerp for that fraction, which is the
/// quaternion exponential map of the shortest-path error. Target motion is fed forward before this
/// correction and is therefore not attenuated by easing. FOV and orthographic height interpolate
/// from their fixed starts using the same eased progress. The first update establishes the initial
/// target sample; target motion is forwarded from the following update onward.
public final class FeedForwardCorrectionTransitionAlgorithm implements TransitionAlgorithm {

  private Blender blender = Blenders::easeInOut;

  private final Vector3d currentPosition = new Vector3d();
  private final Quaternionf currentRotation = new Quaternionf();
  private float startFovDeg = PerspectiveStateImpl.DEFAULT_FOV_DEGREES;
  private float startOrthographicHeight = PerspectiveStateImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT;

  private boolean hasPreviousTarget;
  private float previousEasedProgress;
  private final Vector3d previousTargetPosition = new Vector3d();
  private final Quaternionf previousTargetRotation = new Quaternionf();

  private final Vector3d targetPosition = new Vector3d();
  private final Quaternionf targetRotation = new Quaternionf();
  private final Quaternionf inversePreviousTargetRotation = new Quaternionf();
  private final Quaternionf targetRotationDelta = new Quaternionf();
  private final Quaternionf feedForwardRotation = new Quaternionf();

  public void setBlender(@NonNull Blender blender) {
    this.blender = Objects.requireNonNull(blender);
  }

  public @NonNull Blender getBlender() {
    return blender;
  }

  @Override
  public void start(@NonNull PerspectiveState startState) {
    Objects.requireNonNull(startState);
    currentPosition.set(startState.position());
    currentRotation.set(startState.rotation());
    startFovDeg = startState.getFovDeg();
    startOrthographicHeight = startState.getOrthographicHeight();
    hasPreviousTarget = false;
    previousEasedProgress = 0;
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

    float easedProgress = computeEasedProgress(elapsedTimeMs, durationMs);
    float correctionFraction =
        previousEasedProgress >= 1
            ? 1
            : (easedProgress - previousEasedProgress) / (1 - previousEasedProgress);
    correctionFraction = Utils.clamp(correctionFraction, 0, 1);

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

    dest.position().set(currentPosition);
    dest.rotation().set(currentRotation);
    dest.setFovDeg(lerp(startFovDeg, targetFovDeg, easedProgress));
    dest.setOrthographicHeight(
        lerp(startOrthographicHeight, targetOrthographicHeight, easedProgress));

    previousTargetPosition.set(targetPosition);
    previousTargetRotation.set(targetRotation);
    previousEasedProgress = easedProgress;
    hasPreviousTarget = true;
  }

  private float computeEasedProgress(double elapsedTimeMs, double durationMs) {
    float progress = (float) Utils.clamp(elapsedTimeMs / durationMs, 0, 1);
    float easedProgress = Utils.clamp(blender.blend(progress), 0, 1);
    return Math.max(previousEasedProgress, easedProgress);
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
