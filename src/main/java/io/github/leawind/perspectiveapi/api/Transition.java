package io.github.leawind.perspectiveapi.api;

import org.jspecify.annotations.NonNull;

/// Controls smooth camera transitions between perspectives.
///
/// When a perspective switch occurs, the camera interpolates from its previous position/rotation
/// to the new perspective's values
public interface Transition extends TransitionController {

  /// Starts a new transition from the given start state.
  ///
  /// @param now current timestamp in milliseconds
  /// @param state the start state to transition from
  void start(double now, @NonNull PerspectiveState state);

  /// Updates the interpolated state based on elapsed time.
  ///
  /// @param now current timestamp in milliseconds
  /// @param state target state to transition towards
  void update(double now, @NonNull PerspectiveState state);

  /// @return current interpolated state
  @NonNull PerspectiveState getCurrentState();
}
