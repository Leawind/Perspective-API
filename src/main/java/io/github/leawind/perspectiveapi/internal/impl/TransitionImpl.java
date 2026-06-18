package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.utils.PerspectiveUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

public class TransitionImpl implements Transition {

  // region settings
  private long duration = 200;
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
  public void setBlender(Blender blender) {
    this.blender = blender;
  }

  @Override
  public Blender getBlender() {
    return blender;
  }

  public void start() {
    var camera = getCamera();
    if (camera == null) return;

    startTime = System.currentTimeMillis();
    PerspectiveUtils.extractCameraTransform(camera, startPosition, startRotation);
  }

  public void update(long now, Perspective perspective) {
    var deltaTime = now - startTime;

    float x = blender.blend((float) deltaTime / (float) duration);

    startPosition.lerp(perspective.getPosition(), x, position);
    startRotation.slerp(perspective.getRotation(), x, rotation);
  }

  public Vector3dc getPosition() {
    return position;
  }

  public Quaternionfc getRotation() {
    return rotation;
  }

  private static @Nullable Camera getCamera() {
    var minecraft = Minecraft.getInstance();
    if (minecraft == null) return null;
    var gameRenderer = minecraft.gameRenderer;
    if (gameRenderer == null) return null;
    return gameRenderer.getMainCamera();
  }
}
