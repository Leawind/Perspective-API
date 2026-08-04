package io.github.leawind.perspectiveapi.internal.impl.transition.rotation;

import io.github.leawind.perspectiveapi.internal.utils.Utils;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.jspecify.annotations.NonNull;

/// Chases the current rotation target from the previously produced result.
public final class ChasingRotationTransitionAlgorithm implements RotationTransitionAlgorithm {
  public static final ChasingRotationTransitionAlgorithm INSTANCE =
      new ChasingRotationTransitionAlgorithm();
  private final Quaternionf previousRotation = new Quaternionf();
  private final Quaternionf targetRotation = new Quaternionf();
  private float previousProgress;

  private ChasingRotationTransitionAlgorithm() {}

  @Override
  public @NonNull String id() {
    return "chase_target";
  }

  @Override
  public void start(@NonNull Quaternionfc startRotation) {
    previousRotation.set(Objects.requireNonNull(startRotation));
    previousProgress = 0;
  }

  @Override
  public void update(
      float progress,
      @NonNull Quaternionfc targetRotation,
      @NonNull Quaternionf destRotation) {
    this.targetRotation.set(Objects.requireNonNull(targetRotation));
    Objects.requireNonNull(destRotation);
    progress = Utils.clamp(progress, 0, 1);
    float chaseFraction =
        previousProgress >= 1
            ? 1
            : Utils.clamp((progress - previousProgress) / (1 - previousProgress), 0, 1);

    previousRotation.slerp(this.targetRotation, chaseFraction, destRotation).normalize();
    previousRotation.set(destRotation);
    previousProgress = progress;
  }
}
