package io.github.leawind.perspectiveapi.internal.impl.transition.position;

import io.github.leawind.perspectiveapi.internal.utils.Utils;
import java.util.Objects;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

/// Interpolates position from a fixed start toward the current target.
public final class FixedStartPositionTransitionAlgorithm
    implements PositionTransitionAlgorithm {
  public static final FixedStartPositionTransitionAlgorithm INSTANCE =
      new FixedStartPositionTransitionAlgorithm();

  private final Vector3d startPosition = new Vector3d();
  private final Vector3d targetPosition = new Vector3d();

  private FixedStartPositionTransitionAlgorithm() {}

  @Override
  public @NonNull String id() {
    return "fixed_start";
  }

  @Override
  public void start(@NonNull Vector3dc startPosition) {
    this.startPosition.set(Objects.requireNonNull(startPosition));
  }

  @Override
  public void update(
      float progress,
      @NonNull Vector3dc targetPosition,
      @NonNull Vector3d destPosition) {
    this.targetPosition.set(Objects.requireNonNull(targetPosition));
    Objects.requireNonNull(destPosition);
    startPosition.lerp(this.targetPosition, Utils.clamp(progress, 0, 1), destPosition);
  }
}
