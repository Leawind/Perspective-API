package io.github.leawind.perspectiveapi.internal.impl.transition.scalar;

import org.jspecify.annotations.NonNull;

/// Produces the orthographic-height channel of a fixed-duration perspective transition.
///
/// Implementations own their start snapshot and intermediate state. The caller provides the
/// globally eased transition progress and owns transition-window decisions.
public interface OrthographicHeightTransitionAlgorithm {

  /// Returns the stable identifier used by the debug configuration screen.
  @NonNull String id();

  /// Resets this algorithm for a transition beginning at `startHeight`.
  void start(float startHeight);

  /// Returns the height at globally eased `progress` toward `targetHeight`.
  float update(float progress, float targetHeight);
}
