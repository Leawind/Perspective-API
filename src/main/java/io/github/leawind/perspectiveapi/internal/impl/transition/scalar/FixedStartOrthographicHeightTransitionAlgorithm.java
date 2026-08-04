package io.github.leawind.perspectiveapi.internal.impl.transition.scalar;

import org.jspecify.annotations.NonNull;

/// Interpolates orthographic height from a fixed start toward the current target.
public final class FixedStartOrthographicHeightTransitionAlgorithm
    implements OrthographicHeightTransitionAlgorithm {
  public static final FixedStartOrthographicHeightTransitionAlgorithm INSTANCE =
      new FixedStartOrthographicHeightTransitionAlgorithm();

  private final FixedStartScalarTransition transition = new FixedStartScalarTransition();

  private FixedStartOrthographicHeightTransitionAlgorithm() {}

  @Override
  public @NonNull String id() {
    return "fixed_start";
  }

  @Override
  public void start(float startHeight) {
    transition.start(startHeight);
  }

  @Override
  public float update(float progress, float targetHeight) {
    return transition.update(progress, targetHeight);
  }
}
