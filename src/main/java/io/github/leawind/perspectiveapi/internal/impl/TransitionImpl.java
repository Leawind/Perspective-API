package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

public final class TransitionImpl implements Transition {

  private static final double MIN_DELTA_MS = 1;

  // region settings
  private int durationMs = 300;
  private Blender blender = Blender::easeOut;
  // endregion

  private double startTimeMs;

  // region start state
  private final Vector3d startPosition = new Vector3d();
  private final Quaternionf startRotation = new Quaternionf();
  private float startFov = 70.0f;

  private final Vector3d currentPosition = new Vector3d();
  private final Quaternionf currentRotation = new Quaternionf();
  private float currentFov = 70.0f;

  TransitionImpl() {}

  @Override
  public boolean isInTransition(long currentTimeMs) {
    return currentTimeMs - startTimeMs < durationMs;
  }

  @Override
  public void setDurationMs(int durationMs) {
    this.durationMs = durationMs;
  }

  @Override
  public int getDurationMs() {
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
    this.startFov = startFov;
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
      double currentTimeMs, Vector3dc targetPosition, Quaternionfc targetRotation) {
    float progress = getProgress(currentTimeMs);
    startPosition.lerp(targetPosition, progress, currentPosition);
    startRotation.slerp(targetRotation, progress, currentRotation);
  }

  @Override
  public float updateFov(double currentTimeMs, float targetFov) {
    float progress = getProgress(currentTimeMs);
    return currentFov = startFov + (targetFov - startFov) * progress;
  }

  @Override
  public @NonNull Vector3dc getCurrentPosition() {
    return currentPosition;
  }

  @Override
  public @NonNull Quaternionfc getCurrentRotation() {
    return currentRotation;
  }

  @Override
  public float getCurrentFov() {
    return currentFov;
  }
}
