package io.github.leawind.perspectiveapi.internal.impl.transition.scalar;

import org.jspecify.annotations.NonNull;

/// Produces the FOV channel of a fixed-duration perspective transition.
///
/// Implementations own their start snapshot and intermediate state. The caller provides the
/// globally eased transition progress and owns transition-window decisions.
public interface FovTransitionAlgorithm {

  /// Returns the stable identifier used by the debug configuration screen.
  @NonNull String id();

  /// Resets this algorithm for a transition beginning at `startFovDeg`.
  void start(float startFovDeg);

  /// Returns the FOV at globally eased `progress` toward `targetFovDeg`.
  float update(float progress, float targetFovDeg);
}
