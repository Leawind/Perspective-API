package io.github.leawind.perspectiveapi.internal.impl.transition.rotation;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.jspecify.annotations.NonNull;

/// Produces the rotation channel of a fixed-duration perspective transition.
///
/// Implementations own their start snapshot and intermediate state. The caller provides the
/// globally eased transition progress and owns transition-window decisions.
public interface RotationTransitionAlgorithm {

  /// Returns the stable identifier used by the debug configuration screen.
  @NonNull String id();

  /// Resets this algorithm for a transition beginning at `startRotation`.
  void start(@NonNull Quaternionfc startRotation);

  /// Writes the rotation at globally eased `progress` toward `targetRotation` into `destRotation`.
  void update(
      float progress, @NonNull Quaternionfc targetRotation, @NonNull Quaternionf destRotation);
}
