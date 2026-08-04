package io.github.leawind.perspectiveapi.internal.impl.transition;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import org.jspecify.annotations.NonNull;

/// Produces continuous camera state during a fixed-duration perspective transition.
///
/// Implementations own their start snapshot, interpolation functions, parameters, and all mutable
/// intermediate state. Transition duration and transition-window decisions belong to the caller.
public interface TransitionAlgorithm {

  /// Resets the algorithm for a new transition beginning at `startState`.
  void start(@NonNull PerspectiveState startState);

  /// Writes the state at `elapsedTimeMs` toward the current `target` into `dest`.
  ///
  /// `elapsedTimeMs` is within `[0, durationMs)`. `target` and `dest` may be the same instance.
  void update(
      double elapsedTimeMs,
      double durationMs,
      @NonNull PerspectiveState target,
      PerspectiveState.@NonNull Mutable dest);
}
