package io.github.leawind.perspectiveapi.internal.utils;

import io.github.leawind.perspectiveapi.api.PerspectiveHelper;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class PerspectiveUtils {
  private PerspectiveUtils() {}

  public static void getEntityRotation(Entity entity, float partialTicks, Quaternionf quat) {
    PerspectiveHelper.getQuat(
        new Vec2(entity.getViewXRot(partialTicks), entity.getViewYRot(partialTicks)), quat);
  }

  public static void setCameraPosition(Camera camera, Vector3dc position) {
    // Apply the custom spatial position to the camera.
    CameraAccessor.of(camera).invokeSetPosition(position.x(), position.y(), position.z());
  }

  public static void setCameraRotation(Camera camera, Quaternionfc rotation) {
    // Apply the custom rotation to the camera.
    // refer to net.minecraft.client.Camera#setRotation
    var cameraAdapter = CameraAccessor.of(camera);

    Vector2f orientation = PerspectiveHelper.getEulerDeg(rotation, new Vector2f());

    // #xRot, #yRot: float
    cameraAdapter.setXRot(orientation.x());
    cameraAdapter.setYRot(orientation.y());

    // #rotation: Quaternionf
    cameraAdapter.getRotation().set(rotation);

    // #forwards, #up, #left: Vector3f
    PerspectiveHelper.getForwardVector(rotation, cameraAdapter.perspective_api$forwards());
    PerspectiveHelper.getUpVector(rotation, cameraAdapter.perspective_api$up());
    PerspectiveHelper.getLeftVector(rotation, cameraAdapter.perspective_api$left());

    /*? if >=26.1 {*/
    cameraAdapter.setMatrixPropertiesDirty(cameraAdapter.getMatrixPropertiesDirty() | 3);
    /*? }*/

    //    var eulerDeg = PerspectiveHelper.getEulerDeg(quat, new Vector2f());
    //    cameraAdapter.invokeSetRotation(eulerDeg.y(), eulerDeg.x());
  }

  public static void setCameraRotation(Camera camera, float xRot, float yRot) {
    CameraAccessor.of(camera).invokeSetRotation(yRot, xRot);
  }

  public static void setCameraRotation(Camera camera, Vector2fc eulerDeg) {
    setCameraRotation(camera, eulerDeg.x(), eulerDeg.y());
  }

  public static void setCameraRotation(Camera camera, Vec2 eulerDeg) {
    setCameraRotation(camera, eulerDeg.x, eulerDeg.y);
  }

  public static Vector3d getCameraPosition(Camera camera, Vector3d dest) {
    Vec3 pos = CameraAccessor.of(camera).getPosition();
    dest.set(pos.x(), pos.y(), pos.z());
    return dest;
  }

  public static Quaternionf getCameraRotation(Camera camera, Quaternionf dest) {
    return dest.set(camera.rotation());
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
