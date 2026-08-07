package io.github.leawind.perspectiveapi.internal.impl.transition.position;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

/// Produces the position channel of a fixed-duration perspective transition.
///
/// Implementations own their start snapshot and intermediate state. The caller provides the
/// globally eased transition progress and owns transition-window decisions.
public interface PositionTransitionAlgorithm {

  /// Returns the stable identifier used by the debug configuration screen.
  @NonNull String id();

  /// Resets this algorithm for a transition beginning at `startPosition`.
  void start(@NonNull Vector3dc startPosition);

  /// Writes the position at globally eased `progress` toward `targetPosition` into `destPosition`.
  void update(float progress, @NonNull Vector3dc targetPosition, @NonNull Vector3d destPosition);
}
