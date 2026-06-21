package io.github.leawind.perspectiveapi.internal.utils;

import io.github.leawind.perspectiveapi.api.PerspectiveHelper;
import io.github.leawind.perspectiveapi.internal.bridge.mixin.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class PerspectiveUtils {
  private PerspectiveUtils() {}

  public static void getEntityRotation(Entity entity, float partialTicks, Quaternionf rotation) {
    PerspectiveHelper.getQuat(
        new Vec2(entity.getViewXRot(partialTicks), entity.getViewYRot(partialTicks)), rotation);
  }

  public static void setCameraTransform(Camera camera, Vector3dc position, Quaternionfc rotation) {
    CameraAccessor cameraAccessor = (CameraAccessor) camera;

    // Apply the custom spatial position to the camera.
    {
      cameraAccessor.invokeSetPosition(position.x(), position.y(), position.z());
    }

    // Apply the custom rotation to the camera.
    // refer to net.minecraft.client.Camera#setRotation
    // Refer to Camera#setRotation
    {
      Vector2f orientation = PerspectiveHelper.getEulerDeg(rotation, new Vector2f());

      // #xRot, #yRot: float
      cameraAccessor.setXRot(orientation.x());
      cameraAccessor.setYRot(orientation.y());

      // #rotation: Quaternionf
      cameraAccessor.getRotation().set(rotation);

      // #forwards, #up, #left: Vector3f
      PerspectiveHelper.getForwardVector(rotation, cameraAccessor.getForwards());
      PerspectiveHelper.getUpVector(rotation, cameraAccessor.getUp());
      PerspectiveHelper.getLeftVector(rotation, cameraAccessor.getLeft());

      /*? if >=26.1 {*/
      cameraAccessor.setMatrixPropertiesDirty(cameraAccessor.getMatrixPropertiesDirty() | 3);
      /*? }*/
    }
  }

  public static void extractCameraTransform(
      Camera camera, Vector3d position, Quaternionf rotation) {
    Vec3 pos = ((CameraAccessor) camera).getPosition();
    position.set(pos.x, pos.y, pos.z);

    rotation.set(camera.rotation());
  }

  @SuppressWarnings("ConstantConditions")
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

  /// Copied from `java.lang.Math#clamp`
  public static float clamp(float value, float min, float max) {
    if (!(min < max)) {
      if (Float.isNaN(min)) {
        throw new IllegalArgumentException("min is NaN");
      }
      if (Float.isNaN(max)) {
        throw new IllegalArgumentException("max is NaN");
      }
      if (Float.compare(min, max) > 0) {
        throw new IllegalArgumentException(min + " > " + max);
      }
    }
    return Math.min(max, Math.max(value, min));
  }
}
