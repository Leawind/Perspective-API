package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.ProjectionMode;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.utils.Utils;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blender;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blenders;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/// Controls smooth camera transitions between perspectives.
///
/// Position, FOV, and orthographic height interpolate from the fixed transition start toward the
/// current target. Rotation uses chase interpolation so a moving target remains smooth between
/// rendered frames. Projection mode changes are discrete.
public final class TransitionImpl implements Transition {

  private static final double MIN_DELTA_MS = 0.1;
  private static final float DEFAULT_FOV_DEG = 70.0f;

  // region settings

  private double durationMs = 260;
  private Blender blender = Blenders::easeInOut;
  private double blendPower = 0.6;

  // endregion

  // region start state

  private double startTimeMs;
  private final Vector3d startPosition = new Vector3d();
  private final Quaternionf startRotation = new Quaternionf();
  private float startFovDeg = DEFAULT_FOV_DEG;
  private ProjectionMode startProjectionMode = ProjectionMode.PERSPECTIVE;
  private float startOrthographicHeight = PerspectiveStateImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT;

  // endregion

  // region interpolation state

  private final Quaternionf prevRotation = new Quaternionf();
  private float prevEasedProgress = 0;

  // endregion

  public TransitionImpl() {}

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

  /// Sets the blending function used for easing.
  public void setBlender(@NonNull Blender blender) {
    this.blender = Objects.requireNonNull(blender);
  }

  /// Returns the current blending function.
  public @NonNull Blender getBlender() {
    return blender;
  }

  public void setBlendPower(double blendPower) {
    if (!Double.isFinite(blendPower) || blendPower <= 0) {
      throw new IllegalArgumentException("blendPower must be finite and positive");
    }
    this.blendPower = blendPower;
  }

  public double getBlendPower() {
    return blendPower;
  }

  /// @return progress in `[0, 1]`
  private float computeEasedProgress(double currentTimeMs) {
    double deltaMs = currentTimeMs - startTimeMs;
    deltaMs = Math.max(deltaMs, MIN_DELTA_MS);

    float t = (float) (deltaMs / durationMs);
    t = Utils.clamp(t, 0, 1);
    t = blender.blend(t);
    if (blendPower != 1) {
      t = (float) Math.pow(t, blendPower);
    }
    t = Utils.clamp(t, 0, 1);
    return t;
  }

  /// Starts a new transition from the given start state.
  public void setStartState(double startTimeMs, @NonNull PerspectiveState startState) {
    this.startTimeMs = startTimeMs;
    this.startPosition.set(startState.position());
    this.startRotation.set(startState.rotation());
    this.startFovDeg = startState.getFovDeg();
    this.startProjectionMode = startState.projectionMode();
    this.startOrthographicHeight = startState.getOrthographicHeight();
    this.prevRotation.set(startState.rotation());
    this.prevEasedProgress = 0;
  }

  /// Interpolates continuous camera state from the start state toward the target state and writes
  /// the result into `dest`. Projection mode changes are applied immediately.
  ///
  /// `target` and `dest` may be the same instance.
  public void update(
      double currentTimeMs,
      @NonNull PerspectiveState target,
      PerspectiveState.@NonNull Mutable dest) {
    updateTransform(
        currentTimeMs, target.position(), target.rotation(), dest.position(), dest.rotation());
    dest.setFovDeg(updateFovDeg(currentTimeMs, target.getFovDeg()));
    ProjectionMode targetProjectionMode = target.projectionMode();
    dest.setProjectionMode(targetProjectionMode);
    dest.setOrthographicHeight(
        startProjectionMode == targetProjectionMode
            ? updateOrthographicHeight(currentTimeMs, target.getOrthographicHeight())
            : target.getOrthographicHeight());
  }

  private void updateTransform(
      double currentTimeMs,
      Vector3dc targetPosition,
      Quaternionfc targetRotation,
      Vector3d destPosition,
      Quaternionf destRotation) {
    float easedProgress = computeEasedProgress(currentTimeMs);

    // Position: interpolate from fixed start to dynamic target
    startPosition.lerp(targetPosition, easedProgress, destPosition);

    // Rotation: chase interpolation from previous frame result to dynamic target
    double k = (easedProgress - prevEasedProgress) / (1 - prevEasedProgress);
    k = Utils.clamp(k, 0, 1);

    prevRotation.slerp(targetRotation, (float) k, destRotation);
    prevRotation.set(destRotation);
    prevEasedProgress = easedProgress;
  }

  private float updateFovDeg(double currentTimeMs, float targetFovDeg) {
    float easedProgress = computeEasedProgress(currentTimeMs);
    return startFovDeg + (targetFovDeg - startFovDeg) * easedProgress;
  }

  private float updateOrthographicHeight(
      double currentTimeMs, float targetOrthographicHeight) {
    float easedProgress = computeEasedProgress(currentTimeMs);
    return startOrthographicHeight
        + (targetOrthographicHeight - startOrthographicHeight) * easedProgress;
  }
}
