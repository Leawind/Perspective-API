package io.github.leawind.perspectiveapi.internal.impl.transition;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveStateImpl;
import io.github.leawind.perspectiveapi.internal.utils.Utils;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blender;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blenders;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/// Interpolates position and scalar fields from a fixed start while rotation chases the current
/// target from the previously produced result.
public final class FixedStartChasingRotationTransitionAlgorithm implements TransitionAlgorithm {
  public static final double DEFAULT_BLEND_POWER = 0.6;

  private static final double MIN_ELAPSED_TIME_MS = 0.1;

  private Blender blender = Blenders::easeInOut;
  private double blendPower = DEFAULT_BLEND_POWER;

  private final Vector3d startPosition = new Vector3d();
  private final Quaternionf previousRotation = new Quaternionf();
  private float startFovDeg = PerspectiveStateImpl.DEFAULT_FOV_DEGREES;
  private float startOrthographicHeight = PerspectiveStateImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT;
  private float previousProgress;

  public void setBlender(@NonNull Blender blender) {
    this.blender = Objects.requireNonNull(blender);
  }

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

  @Override
  public void start(@NonNull PerspectiveState startState) {
    Objects.requireNonNull(startState);
    startPosition.set(startState.position());
    previousRotation.set(startState.rotation());
    startFovDeg = startState.getFovDeg();
    startOrthographicHeight = startState.getOrthographicHeight();
    previousProgress = 0;
  }

  @Override
  public void update(
      double elapsedTimeMs,
      double durationMs,
      @NonNull PerspectiveState target,
      PerspectiveState.@NonNull Mutable dest) {
    Objects.requireNonNull(target);
    Objects.requireNonNull(dest);
    float progress = computeProgress(elapsedTimeMs, durationMs);

    startPosition.lerp(target.position(), progress, dest.position());

    double chaseFactor = (progress - previousProgress) / (1 - previousProgress);
    chaseFactor = Utils.clamp(chaseFactor, 0, 1);
    previousRotation.slerp(target.rotation(), (float) chaseFactor, dest.rotation());
    previousRotation.set(dest.rotation());
    previousProgress = progress;

    dest.setFovDeg(lerp(startFovDeg, target.getFovDeg(), progress));
    dest.setOrthographicHeight(
        lerp(startOrthographicHeight, target.getOrthographicHeight(), progress));
  }

  private float computeProgress(double elapsedTimeMs, double durationMs) {
    double adjustedElapsedTimeMs = Math.max(elapsedTimeMs, MIN_ELAPSED_TIME_MS);
    float progress = (float) (adjustedElapsedTimeMs / durationMs);
    progress = Utils.clamp(progress, 0, 1);
    progress = blender.blend(progress);
    if (blendPower != 1) {
      progress = (float) Math.pow(progress, blendPower);
    }
    return Utils.clamp(progress, 0, 1);
  }

  private static float lerp(float start, float target, float progress) {
    return start + (target - start) * progress;
  }
}
