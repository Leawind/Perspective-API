package io.github.leawind.perspectiveapi.internal.impl.transition.scalar;

import io.github.leawind.perspectiveapi.internal.utils.Utils;

/// Shared scalar calculation used by channel-specific transition algorithm singletons.
final class FixedStartScalarTransition {
  private float startValue;

  FixedStartScalarTransition() {}

  void start(float startValue) {
    this.startValue = startValue;
  }

  float update(float progress, float targetValue) {
    progress = Utils.clamp(progress, 0, 1);
    return startValue + (targetValue - startValue) * progress;
  }
}
