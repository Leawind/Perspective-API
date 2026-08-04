package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.ProjectionMode;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.impl.transition.TransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.TransitionAlgorithmType;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/// Controls smooth camera transitions between perspectives.
///
/// Owns the fixed transition window and delegates continuous-state interpolation to the selected
/// algorithm. Projection mode changes are discrete.
public final class TransitionImpl implements Transition {
  public static final double DEFAULT_DURATION_MS = 260.0;
  public static final TransitionAlgorithmType DEFAULT_ALGORITHM =
      TransitionAlgorithmType.FEED_FORWARD_CORRECTION;

  private double durationMs = DEFAULT_DURATION_MS;
  private double startTimeMs;
  private final PerspectiveStateImpl startState = new PerspectiveStateImpl();
  private boolean hasStartState;
  private TransitionAlgorithmType algorithmType = DEFAULT_ALGORITHM;
  private TransitionAlgorithm algorithm = DEFAULT_ALGORITHM.create();

  public static double getTimeMs() {
    return GLFW.glfwGetTime() * 1000;
  }

  /// Returns `true` if a transition is currently in progress at the given timestamp.
  public boolean isInTransition(double currentTimeMs) {
    return currentTimeMs - startTimeMs < durationMs;
  }

  @Override
  public void setDurationMs(double durationMs) {
    if (!Double.isFinite(durationMs) || durationMs < 0) {
      throw new IllegalArgumentException("durationMs must be finite and non-negative");
    }
    this.durationMs = durationMs;
  }

  @Override
  public double getDurationMs() {
    return durationMs;
  }

  /// Returns the selected internal interpolation algorithm.
  public @NonNull TransitionAlgorithm algorithm() {
    return algorithm;
  }

  public @NonNull TransitionAlgorithmType getAlgorithmType() {
    return algorithmType;
  }

  /// Changes the interpolation algorithm without changing the fixed transition window.
  ///
  /// If a transition has already been initialized, the new algorithm restarts from that
  /// transition's original start state and immediately takes over subsequent updates.
  public void setAlgorithmType(@NonNull TransitionAlgorithmType algorithmType) {
    Objects.requireNonNull(algorithmType);
    if (this.algorithmType == algorithmType) return;
    this.algorithmType = algorithmType;
    algorithm = algorithmType.create();
    if (hasStartState) algorithm.start(startState);
  }

  /// Starts a new transition from the given start state.
  public void setStartState(double startTimeMs, @NonNull PerspectiveState startState) {
    this.startTimeMs = startTimeMs;
    this.startState.set(Objects.requireNonNull(startState));
    hasStartState = true;
    algorithm.start(this.startState);
  }

  /// Interpolates continuous camera state from the start state toward the target state and writes
  /// the result into `dest`. Projection mode changes are applied immediately.
  ///
  /// `target` and `dest` may be the same instance.
  public void update(
      double currentTimeMs,
      @NonNull PerspectiveState target,
      PerspectiveState.@NonNull Mutable dest) {
    Objects.requireNonNull(target);
    Objects.requireNonNull(dest);
    double elapsedTimeMs = Math.max(0, currentTimeMs - startTimeMs);
    if (durationMs == 0 || elapsedTimeMs >= durationMs) {
      copyContinuousState(target, dest);
    } else {
      algorithm.update(elapsedTimeMs, durationMs, target, dest);
    }
    ProjectionMode targetProjectionMode = target.projectionMode();
    dest.setProjectionMode(targetProjectionMode);
  }

  private static void copyContinuousState(
      @NonNull PerspectiveState source, PerspectiveState.@NonNull Mutable destination) {
    destination.position().set(source.position());
    destination.rotation().set(source.rotation());
    destination.setFovDeg(source.getFovDeg());
    destination.setOrthographicHeight(source.getOrthographicHeight());
  }
}
