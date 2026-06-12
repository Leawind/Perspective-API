package io.github.leawind.perspectiveapi.utils;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveHelper;
import io.github.leawind.perspectiveapi.internal.bridge.mixin.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class PerspectiveUtils {
  private PerspectiveUtils() {}

  public static void applyPerspectiveToCamera(Perspective perspective, Camera camera) {
    CameraAccessor cameraAccessor = (CameraAccessor) camera;

    // Apply the custom spatial position to the camera.
    {
      Vector3dc perspectivePosition = perspective.getPosition();
      cameraAccessor.invokeSetPosition(
          perspectivePosition.x(), perspectivePosition.y(), perspectivePosition.z());
    }

    // Apply the custom orientation to the camera.
    // refer to net.minecraft.client.Camera#setRotation
    {
      Quaternionfc perspectiveRotation = perspective.getRotation();

      // update field rotation: Quaternionf
      cameraAccessor.getRotation().set(perspectiveRotation);

      // update field xRot, yRot
      Vector2f orientation = PerspectiveHelper.getOrientation(perspectiveRotation, new Vector2f());
      cameraAccessor.setXRot(orientation.x());
      cameraAccessor.setYRot(orientation.y());

      // update field forwards, up, left
      PerspectiveHelper.getForwardVector(perspectiveRotation, cameraAccessor.getForwards());
      PerspectiveHelper.getUpVector(perspectiveRotation, cameraAccessor.getUp());
      PerspectiveHelper.getLeftVector(perspectiveRotation, cameraAccessor.getLeft());
    }
  }

  public static void extractCameraTransform(
      Camera camera, Vector3d position, Quaternionf rotation) {
    Vec3 pos = ((CameraAccessor) camera).getPosition();
    position.set(pos.x, pos.y, pos.z);

    rotation.set(camera.rotation());
  }

  public static void updateCameraType(CameraType newCameraType) {
    // similar to vanilla: `Minecraft#handleKeybinds()`

    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft == null || minecraft.options == null || minecraft.gameRenderer == null) {
      return;
    }

    var oldCameraType = minecraft.options.getCameraType();
    if (oldCameraType.isFirstPerson() != newCameraType.isFirstPerson()) {
      minecraft.gameRenderer.checkEntityPostEffect(
          newCameraType.isFirstPerson() ? minecraft.getCameraEntity() : null);
    }
    minecraft.options.setCameraType(newCameraType);

    var levelRenderer = minecraft.levelRenderer;
    if (levelRenderer != null) {
      levelRenderer.needsUpdate();
    }
  }
}
