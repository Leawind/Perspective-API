package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

public final class TransitionImpl implements Transition {

  private static final double MIN_DELTA_MS = 0.1;

  // region settings
  private double durationMs = 300;
  private Blender blender = Blender::easeOut;
  // endregion

  private double startTimeMs;

  // region start state

  private final Vector3d startPosition = new Vector3d();
  private final Quaternionf startRotation = new Quaternionf();
  private float startFov = 70.0f;

  // endregion

  // region rotation transition state

  private final Quaternionf prevRotation = new Quaternionf();
  private float prevEasedProgress = 0;

  // endregion

  TransitionImpl() {}

  @Override
  public boolean isInTransition(double currentTimeMs) {
    return currentTimeMs - startTimeMs < durationMs;
  }

  @Override
  public void setDurationMs(double durationMs) {
    this.durationMs = durationMs;
  }

  @Override
  public double getDurationMs() {
    return durationMs;
  }

  @Override
  public void setBlender(@NonNull Blender blender) {
    this.blender = Objects.requireNonNull(blender);
  }

  @Override
  public @NonNull Blender getBlender() {
    return blender;
  }

  @Override
  public void setStartState(
      double startTimeMs, Vector3dc startPosition, Quaternionfc startRotation, float startFov) {
    this.startTimeMs = startTimeMs;
    this.startPosition.set(startPosition);
    this.startRotation.set(startRotation);
    this.prevRotation.set(startRotation);
    this.startFov = startFov;
    this.prevEasedProgress = 0;
  }

  private float getProgress(double currentTimeMs) {
    double deltaMs = currentTimeMs - startTimeMs;
    deltaMs = Math.max(deltaMs, MIN_DELTA_MS);

    float t = (float) (deltaMs / durationMs);
    t = PerspectiveUtils.clamp(t, 0, 1);
    t = blender.blend(t);
    t = PerspectiveUtils.clamp(t, 0, 1);
    return t;
  }

  @Override
  public void updateTransform(
      double currentTimeMs,
      Vector3dc targetPosition,
      Quaternionfc targetRotation,
      Vector3d destPosition,
      Quaternionf destRotation) {
    float progress = getProgress(currentTimeMs);

    // Position: interpolate from fixed start to dynamic target
    startPosition.lerp(targetPosition, progress, destPosition);

    // Rotation: chase interpolation from previous frame result to dynamic target
    float u = getProgress(currentTimeMs);
    double k = (u - prevEasedProgress) / (1 - prevEasedProgress);
    k = PerspectiveUtils.clamp(k, 0, 1);

    prevRotation.slerp(targetRotation, (float) k, destRotation);
    prevRotation.set(destRotation);
    prevEasedProgress = u;
  }

  @Override
  public float updateFov(double currentTimeMs, float targetFov) {
    float progress = getProgress(currentTimeMs);
    return startFov + (targetFov - startFov) * progress;
  }
}
