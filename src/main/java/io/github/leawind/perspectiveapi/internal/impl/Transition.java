package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.utils.PerspectiveUtils;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class Transition {
  // region settings

  private final long duration;

  // endregion

  private long startTime;
  private final Vector3d startPosition = new Vector3d();
  private final Quaternionf startRotation = new Quaternionf();

  private final Vector3d position = new Vector3d();
  private final Quaternionf rotation = new Quaternionf();

  public Transition() {
    this(100);
  }

  public Transition(long duration) {
    this.duration = duration;
  }

  public void start() {
    var minecraft = Minecraft.getInstance();
    if (minecraft == null) return;
    var gameRenderer = minecraft.gameRenderer;
    if (gameRenderer == null) return;
    var camera = gameRenderer.getMainCamera();
    if (camera == null) return;

    startTime = System.currentTimeMillis();
    PerspectiveUtils.extractCameraTransform(camera, startPosition, startRotation);
  }

  public boolean isInTransition(long now) {
    return now - startTime < duration;
  }

  public void update(long now, Perspective perspective) {
    var deltaTime = now - startTime;

    // TODO func
    float x = (float) deltaTime / (float) duration;
    x = 3 * x * x - 2 * x * x * x;

    startPosition.lerp(perspective.getPosition(), x, position);
    startRotation.slerp(perspective.getRotation(), x, rotation);
  }

  public Vector3dc getPosition() {
    return position;
  }

  public Quaternionfc getRotation() {
    return rotation;
  }
}
