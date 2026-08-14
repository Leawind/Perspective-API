package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.ProjectionMode;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.impl.transition.position.FeedForwardCorrectionPositionTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.position.PositionTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.rotation.FeedForwardCorrectionRotationTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.rotation.RotationTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.FixedStartFovTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.FixedStartOrthographicHeightTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.FovTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.OrthographicHeightTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.utils.Utils;
import io.github.leawind.perspectiveapi.internal.utils.smooth.EasingFunctions;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/// Controls smooth camera transitions between perspectives.
///
/// Owns the fixed transition window and delegates every continuous state channel to its selected
/// algorithm. Projection mode changes are discrete.
public final class TransitionImpl implements Transition {
  public static final double DEFAULT_DURATION_MS = 260.0;
  public static final PositionTransitionAlgorithm DEFAULT_POSITION_ALGORITHM =
      FeedForwardCorrectionPositionTransitionAlgorithm.INSTANCE;
  public static final RotationTransitionAlgorithm DEFAULT_ROTATION_ALGORITHM =
      FeedForwardCorrectionRotationTransitionAlgorithm.INSTANCE;
  public static final FovTransitionAlgorithm DEFAULT_FOV_ALGORITHM =
      FixedStartFovTransitionAlgorithm.INSTANCE;
  public static final OrthographicHeightTransitionAlgorithm DEFAULT_ORTHOGRAPHIC_HEIGHT_ALGORITHM =
      FixedStartOrthographicHeightTransitionAlgorithm.INSTANCE;

  private double durationMs = DEFAULT_DURATION_MS;
  private Easing easing = EasingFunctions::easeOut;
  private double startTimeMs;
  private final PerspectiveStateImpl startState = new PerspectiveStateImpl();
  private boolean hasStartState;
  private PositionTransitionAlgorithm positionAlgorithm = DEFAULT_POSITION_ALGORITHM;
  private RotationTransitionAlgorithm rotationAlgorithm = DEFAULT_ROTATION_ALGORITHM;
  private FovTransitionAlgorithm fovAlgorithm = DEFAULT_FOV_ALGORITHM;
  private OrthographicHeightTransitionAlgorithm orthographicHeightAlgorithm =
      DEFAULT_ORTHOGRAPHIC_HEIGHT_ALGORITHM;

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

  @Override
  public void setEasing(@NonNull Easing easing) {
    this.easing = Objects.requireNonNull(easing);
  }

  @Override
  public @NonNull Easing getEasing() {
    return easing;
  }

  public @NonNull PositionTransitionAlgorithm getPositionAlgorithm() {
    return positionAlgorithm;
  }

  public @NonNull RotationTransitionAlgorithm getRotationAlgorithm() {
    return rotationAlgorithm;
  }

  public @NonNull FovTransitionAlgorithm getFovAlgorithm() {
    return fovAlgorithm;
  }

  public @NonNull OrthographicHeightTransitionAlgorithm getOrthographicHeightAlgorithm() {
    return orthographicHeightAlgorithm;
  }

  /// Changes the position algorithm without changing the fixed transition window.
  ///
  /// If a transition has already been initialized, the new algorithm restarts from that
  /// transition's original start position and immediately takes over subsequent updates.
  public void setPositionAlgorithm(@NonNull PositionTransitionAlgorithm algorithm) {
    Objects.requireNonNull(algorithm);
    if (positionAlgorithm == algorithm) return;
    positionAlgorithm = algorithm;
    if (hasStartState) positionAlgorithm.start(startState.position());
  }

  /// Changes the rotation algorithm without changing the fixed transition window.
  ///
  /// If a transition has already been initialized, the new algorithm restarts from that
  /// transition's original start rotation and immediately takes over subsequent updates.
  public void setRotationAlgorithm(@NonNull RotationTransitionAlgorithm algorithm) {
    Objects.requireNonNull(algorithm);
    if (rotationAlgorithm == algorithm) return;
    rotationAlgorithm = algorithm;
    if (hasStartState) rotationAlgorithm.start(startState.rotation());
  }

  /// Changes the FOV algorithm without changing the fixed transition window.
  ///
  /// If a transition has already been initialized, the new algorithm restarts from that
  /// transition's original FOV and immediately takes over subsequent updates.
  public void setFovAlgorithm(@NonNull FovTransitionAlgorithm algorithm) {
    Objects.requireNonNull(algorithm);
    if (fovAlgorithm == algorithm) return;
    fovAlgorithm = algorithm;
    if (hasStartState) fovAlgorithm.start(startState.getFovDeg());
  }

  /// Changes the orthographic-height algorithm without changing the fixed transition window.
  ///
  /// If a transition has already been initialized, the new algorithm restarts from that
  /// transition's original orthographic height and immediately takes over subsequent updates.
  public void setOrthographicHeightAlgorithm(
      @NonNull OrthographicHeightTransitionAlgorithm algorithm) {
    Objects.requireNonNull(algorithm);
    if (orthographicHeightAlgorithm == algorithm) return;
    orthographicHeightAlgorithm = algorithm;
    if (hasStartState) orthographicHeightAlgorithm.start(startState.getOrthographicHeight());
  }

  /// Starts a new transition from the given start state.
  public void setStartState(double startTimeMs, @NonNull PerspectiveState startState) {
    this.startTimeMs = startTimeMs;
    this.startState.set(Objects.requireNonNull(startState));
    hasStartState = true;
    positionAlgorithm.start(this.startState.position());
    rotationAlgorithm.start(this.startState.rotation());
    fovAlgorithm.start(this.startState.getFovDeg());
    orthographicHeightAlgorithm.start(this.startState.getOrthographicHeight());
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
      float progress = computeEasedProgress(elapsedTimeMs);
      positionAlgorithm.update(progress, target.position(), dest.position());
      rotationAlgorithm.update(progress, target.rotation(), dest.rotation());
      dest.setFovDeg(fovAlgorithm.update(progress, target.getFovDeg()));
      dest.setOrthographicHeight(
          orthographicHeightAlgorithm.update(progress, target.getOrthographicHeight()));
    }
    ProjectionMode targetProjectionMode = target.getProjectionMode();
    dest.setProjectionMode(targetProjectionMode);
  }

  private float computeEasedProgress(double elapsedTimeMs) {
    float progress = (float) Utils.clamp(elapsedTimeMs / durationMs, 0, 1);
    float easedProgress = easing.ease(progress);
    if (!Float.isFinite(easedProgress)) return 0;
    return Utils.clamp(easedProgress, 0, 1);
  }

  private static void copyContinuousState(
      @NonNull PerspectiveState source, PerspectiveState.@NonNull Mutable destination) {
    destination.position().set(source.position());
    destination.rotation().set(source.rotation());
    destination.setFovDeg(source.getFovDeg());
    destination.setOrthographicHeight(source.getOrthographicHeight());
  }
}
