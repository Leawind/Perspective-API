package io.github.leawind.perspectiveapi.internal.impl.transition.scalar;

import org.jspecify.annotations.NonNull;

/// Interpolates FOV from a fixed start toward the current target.
public final class FixedStartFovTransitionAlgorithm implements FovTransitionAlgorithm {
  public static final FixedStartFovTransitionAlgorithm INSTANCE =
      new FixedStartFovTransitionAlgorithm();

  private final FixedStartScalarTransition transition = new FixedStartScalarTransition();

  private FixedStartFovTransitionAlgorithm() {}

  @Override
  public @NonNull String id() {
    return "fixed_start";
  }

  @Override
  public void start(float startFovDeg) {
    transition.start(startFovDeg);
  }

  @Override
  public float update(float progress, float targetFovDeg) {
    return transition.update(progress, targetFovDeg);
  }
}
