package io.github.leawind.perspectiveapi.internal.impl.transition.rotation;

import io.github.leawind.perspectiveapi.internal.utils.Utils;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.jspecify.annotations.NonNull;

/// Feeds world-space target rotation changes directly into the camera while correcting the
/// remaining difference within the fixed transition duration.
public final class FeedForwardCorrectionRotationTransitionAlgorithm
    implements RotationTransitionAlgorithm {
  public static final FeedForwardCorrectionRotationTransitionAlgorithm INSTANCE =
      new FeedForwardCorrectionRotationTransitionAlgorithm();

  private final Quaternionf currentRotation = new Quaternionf();
  private boolean hasPreviousTarget;
  private float previousEasedProgress;
  private final Quaternionf previousTargetRotation = new Quaternionf();
  private final Quaternionf targetRotation = new Quaternionf();
  private final Quaternionf inversePreviousTargetRotation = new Quaternionf();
  private final Quaternionf targetRotationDelta = new Quaternionf();
  private final Quaternionf feedForwardRotation = new Quaternionf();

  private FeedForwardCorrectionRotationTransitionAlgorithm() {}

  @Override
  public @NonNull String id() {
    return "feed_forward_correction";
  }

  @Override
  public void start(@NonNull Quaternionfc startRotation) {
    currentRotation.set(Objects.requireNonNull(startRotation));
    hasPreviousTarget = false;
    previousEasedProgress = 0;
  }

  @Override
  public void update(
      float progress, @NonNull Quaternionfc targetRotation, @NonNull Quaternionf destRotation) {
    this.targetRotation.set(Objects.requireNonNull(targetRotation));
    Objects.requireNonNull(destRotation);

    float easedProgress = Math.max(previousEasedProgress, Utils.clamp(progress, 0, 1));
    float correctionFraction = correctionFraction(easedProgress);

    alignTargetRotationSign();
    if (hasPreviousTarget) {
      previousTargetRotation.invert(inversePreviousTargetRotation);
      this.targetRotation.mul(inversePreviousTargetRotation, targetRotationDelta).normalize();
      targetRotationDelta.mul(currentRotation, feedForwardRotation).normalize();
      currentRotation.set(feedForwardRotation);
    }
    currentRotation.slerp(this.targetRotation, correctionFraction).normalize();

    destRotation.set(currentRotation);
    previousTargetRotation.set(this.targetRotation);
    previousEasedProgress = easedProgress;
    hasPreviousTarget = true;
  }

  private float correctionFraction(float easedProgress) {
    if (previousEasedProgress >= 1) return 1;
    return Utils.clamp((easedProgress - previousEasedProgress) / (1 - previousEasedProgress), 0, 1);
  }

  private void alignTargetRotationSign() {
    Quaternionf reference = hasPreviousTarget ? previousTargetRotation : currentRotation;
    if (reference.dot(targetRotation) < 0) {
      targetRotation.mul(-1);
    }
  }
}
