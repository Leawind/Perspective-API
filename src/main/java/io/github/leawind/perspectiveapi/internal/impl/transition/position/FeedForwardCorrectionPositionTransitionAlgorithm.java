package io.github.leawind.perspectiveapi.internal.impl.transition.position;

import io.github.leawind.perspectiveapi.internal.utils.Utils;
import java.util.Objects;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

/// Feeds target translation directly into the camera while correcting the remaining difference
/// within the fixed transition duration.
public final class FeedForwardCorrectionPositionTransitionAlgorithm
    implements PositionTransitionAlgorithm {
  public static final FeedForwardCorrectionPositionTransitionAlgorithm INSTANCE =
      new FeedForwardCorrectionPositionTransitionAlgorithm();

  private final Vector3d currentPosition = new Vector3d();
  private boolean hasPreviousTarget;
  private float previousEasedProgress;
  private final Vector3d previousTargetPosition = new Vector3d();
  private final Vector3d targetPosition = new Vector3d();

  private FeedForwardCorrectionPositionTransitionAlgorithm() {}

  @Override
  public @NonNull String id() {
    return "feed_forward_correction";
  }

  @Override
  public void start(@NonNull Vector3dc startPosition) {
    currentPosition.set(Objects.requireNonNull(startPosition));
    hasPreviousTarget = false;
    previousEasedProgress = 0;
  }

  @Override
  public void update(
      float progress,
      @NonNull Vector3dc targetPosition,
      @NonNull Vector3d destPosition) {
    this.targetPosition.set(Objects.requireNonNull(targetPosition));
    Objects.requireNonNull(destPosition);

    float easedProgress = Math.max(previousEasedProgress, Utils.clamp(progress, 0, 1));
    float correctionFraction = correctionFraction(easedProgress);
    if (hasPreviousTarget) {
      currentPosition.add(this.targetPosition).sub(previousTargetPosition);
    }
    currentPosition.lerp(this.targetPosition, correctionFraction);

    destPosition.set(currentPosition);
    previousTargetPosition.set(this.targetPosition);
    previousEasedProgress = easedProgress;
    hasPreviousTarget = true;
  }

  private float correctionFraction(float easedProgress) {
    if (previousEasedProgress >= 1) return 1;
    return Utils.clamp((easedProgress - previousEasedProgress) / (1 - previousEasedProgress), 0, 1);
  }
}
