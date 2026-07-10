package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.utils.Utils;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blender;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/// Controls smooth camera transitions between perspectives.
///
/// Supports two interpolation algorithms switchable via `useNewAlgorithm`:
/// - **Old** (default): Chase interpolation — each frame steps from the previous result
///   toward the current target, with step size driven by progress increment.
/// - **New**: Delta synthesis — computes a fixed delta on the first frame, then
///   synthesizes interpolation from the delta and the dynamic target.
public final class TransitionImpl implements Transition {

  private static final double MIN_DELTA_MS = 0.1;

  // region settings

  private double durationMs = 300;
  private Blender blender = Blenders::easeInOut;
  private boolean useNewAlgorithm = false;

  // endregion

  // region start state

  private double startTimeMs;
  private final Vector3d startPosition = new Vector3d();
  private final Quaternionf startRotation = new Quaternionf();
  private float startFov = 70.0f;

  // endregion

  // region old algorithm state

  private final Quaternionf prevRotation = new Quaternionf();
  private float prevEasedProgress = 0;

  // endregion

  // region new algorithm state

  private boolean isDeltaTransformSet = false;
  private boolean isDeltaFovSet = false;
  private final Vector3d deltaPosition = new Vector3d();
  private final Quaternionf deltaRotation = new Quaternionf();
  private float deltaFov = 70.0f;

  // endregion

  public TransitionImpl() {}

  public static double getTimeMs() {
    return GLFW.glfwGetTime() * 1000;
  }

  /// Returns `true` if a transition is currently in progress at the given timestamp.
  public boolean isInTransition(double currentTimeMs) {
    return currentTimeMs - startTimeMs < durationMs;
  }

  public boolean isUseNewAlgorithm() {
    return useNewAlgorithm;
  }

  public void setUseNewAlgorithm(boolean useNewAlgorithm) {
    this.useNewAlgorithm = useNewAlgorithm;
  }

  @Override
  public void setDurationMs(double durationMs) {
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

  /// @return progress in `[0, 1]`
  private float computeEasedProgress(double currentTimeMs) {
    double deltaMs = currentTimeMs - startTimeMs;
    deltaMs = Math.max(deltaMs, MIN_DELTA_MS);

    float t = (float) (deltaMs / durationMs);
    t = Utils.clamp(t, 0, 1);
    t = blender.blend(t);
    t = Utils.clamp(t, 0, 1);
    return t;
  }

  /// Starts a new transition from the given start state.
  ///
  /// @param startTimeMs Current timestamp in milliseconds.
  /// @param startPosition The start position to transition from.
  /// @param startRotation The start rotation to transition from.
  /// @param startFov The start FOV to transition from.
  public void setStartState(
      double startTimeMs, Vector3dc startPosition, Quaternionfc startRotation, float startFov) {
    this.startTimeMs = startTimeMs;
    this.startPosition.set(startPosition);
    this.startRotation.set(startRotation);
    this.startFov = startFov;

    // old algorithm state
    this.prevRotation.set(startRotation);
    this.prevEasedProgress = 0;

    // new algorithm state
    this.isDeltaTransformSet = false;
    this.isDeltaFovSet = false;
  }

  /// Updates the current interpolated position and rotation based on the target state.
  ///
  /// @param currentTimeMs Current timestamp in milliseconds.
  /// @param targetPosition The target position to interpolate towards.
  /// @param targetRotation The target rotation to interpolate towards.
  /// @param destPosition The destination position to write the interpolated position to.
  /// @param destRotation The destination rotation to write the interpolated rotation to.
  public void updateTransform(
      double currentTimeMs,
      Vector3dc targetPosition,
      Quaternionfc targetRotation,
      Vector3d destPosition,
      Quaternionf destRotation) {
    if (useNewAlgorithm) {
      updateTransformNew(currentTimeMs, targetPosition, targetRotation, destPosition, destRotation);
    } else {
      updateTransformOld(currentTimeMs, targetPosition, targetRotation, destPosition, destRotation);
    }
  }

  private void updateTransformOld(
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

  private void updateTransformNew(
      double currentTimeMs,
      Vector3dc targetPosition,
      Quaternionfc targetRotation,
      Vector3d destPosition,
      Quaternionf destRotation) {
    if (!isDeltaTransformSet) {
      targetPosition.sub(startPosition, deltaPosition);
      targetRotation.mul(startRotation.conjugate(deltaRotation), deltaRotation);
      isDeltaTransformSet = true;
    }

    float easedProgress = computeEasedProgress(currentTimeMs);

    // Position: interpolate from fixed start to dynamic target
    startPosition.lerp(targetPosition, easedProgress, destPosition);

    // Rotation: chase interpolation from previous frame result to dynamic target
    deltaRotation.conjugate(startRotation).mul(targetRotation, startRotation);
    targetRotation.slerp(startRotation, easedProgress - 1, destRotation);
  }

  /// Updates and returns the current interpolated FOV based on the target FOV.
  ///
  /// @param currentTimeMs Current timestamp in milliseconds.
  /// @param targetFov The target FOV to interpolate towards.
  /// @return The interpolated FOV.
  public float updateFov(double currentTimeMs, float targetFov) {
    float easedProgress = computeEasedProgress(currentTimeMs);
    if (useNewAlgorithm) {
      if (!isDeltaFovSet) {
        deltaFov = targetFov - startFov;
        isDeltaFovSet = true;
      }
      return targetFov - deltaFov * (1 - easedProgress);
    }
    return startFov + (targetFov - startFov) * easedProgress;
  }
}
