package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TransitionImpl implements Transition {

  private static final long MIN_DELTA_MS = 1;

  // region settings
  private long duration = 300;
  private Blender blender = Blender::easeOut;
  // endregion

  // region start state
  private long startTime;
  private final Vector3d startPosition = new Vector3d();
  private final Quaternionf startRotation = new Quaternionf();
  // endregion

  // region current state
  private final Vector3d position = new Vector3d();
  private final Quaternionf rotation = new Quaternionf();

  // endregion

  @Override
  public boolean isInTransition(long now) {
    return now - startTime < duration;
  }

  @Override
  public void setDuration(long duration) {
    this.duration = duration;
  }

  @Override
  public void setBlender(@NonNull Blender blender) {
    this.blender = Objects.requireNonNull(blender);
  }

  @Override
  public @NonNull Blender getBlender() {
    return blender;
  }

  public void start(long now) {
    var camera = getCamera();
    if (camera == null) return;

    startTime = now;
    PerspectiveUtils.extractCameraTransform(camera, startPosition, startRotation);
  }

  public void update(long now, Perspective perspective) {
    long deltaMs = now - startTime;
    deltaMs = Math.max(deltaMs, MIN_DELTA_MS);

    float x = (float) deltaMs / (float) duration;
    x = PerspectiveUtils.clamp(x, 0, 1);
    x = blender.blend(x);

    startPosition.lerp(perspective.getPosition(), x, position);
    startRotation.slerp(perspective.getRotation(), x, rotation);
  }

  public Vector3dc getPosition() {
    return position;
  }

  public Quaternionfc getRotation() {
    return rotation;
  }

  @SuppressWarnings("ConstantConditions")
  private static @Nullable Camera getCamera() {
    var minecraft = Minecraft.getInstance();
    if (minecraft == null) return null;
    var gameRenderer = minecraft.gameRenderer;
    if (gameRenderer == null) return null;
    return gameRenderer.getMainCamera();
  }
}
