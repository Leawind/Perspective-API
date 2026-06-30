package io.github.leawind.perspectiveapi.internal.bridge;

import io.github.leawind.perspectiveapi.api.PerspectiveHelper;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyFieldOfViewContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2fc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.Nullable;

public final class Bridge {
  private Bridge() {}

  /// Returns the current Minecraft data version number.
  public static int getDataVersion() {
    /*? if >=1.21.11 {*/
    return SharedConstants.getCurrentVersion().dataVersion().version();
    /*? } else {*/
    /*return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
     */
    /*? }*/
  }

  public static Identifier createIdentifier(String path) {
    return createIdentifier("minecraft", path);
  }

  public static Identifier createIdentifier(String namespace, String path) {
    /*? if >=1.21 {*/
    return Identifier.fromNamespaceAndPath(namespace, path);
    /*? } else {*/
    /*return new Identifier(namespace, path);
     */
    /*? }*/
  }

  /// Updates the camera type and triggers necessary side effects.
  ///
  /// Handles post-effect checks and renderer updates when switching between first-person
  /// and third-person views.
  ///
  /// @param newCameraType the new camera type to set
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
    /*? if <26.2 {*/
    /*var levelRenderer = minecraft.levelRenderer;
    if (levelRenderer != null) {
      levelRenderer.needsUpdate();
    }
    */
    /*? }*/
  }

  /// Gets the main camera instance.
  ///
  /// @return the main camera, or null if unavailable
  @SuppressWarnings("ConstantConditions")
  public static @Nullable Camera getMainCamera() {
    var minecraft = Minecraft.getInstance();
    if (minecraft == null) return null;
    var gameRenderer = minecraft.gameRenderer;
    if (gameRenderer == null) return null;
    /*? if >=26.2 {*/
    return gameRenderer.mainCamera();
    /*? } else {*/
    /*return gameRenderer.getMainCamera();
     */
    /*? }*/
  }

  /// Extracts entity rotation as a quaternion.
  ///
  /// @param entity the entity to query
  /// @param partialTicks interpolation factor
  /// @param quat destination quaternion
  public static void getEntityRotation(Entity entity, float partialTicks, Quaternionf quat) {
    PerspectiveHelper.eulerDegToQuat(
        new Vec2(entity.getViewXRot(partialTicks), entity.getViewYRot(partialTicks)), quat);
  }

  /// Gets camera position in world coordinates.
  ///
  /// @param camera the camera to query
  /// @param dest destination vector
  /// @return the destination vector for chaining
  public static Vector3d getCameraPosition(Camera camera, Vector3d dest) {
    Vec3 pos = CameraAccessor.of(camera).getPosition();
    dest.set(pos.x(), pos.y(), pos.z());
    return dest;
  }

  /// Gets camera rotation as a quaternion.
  ///
  /// @param camera the camera to query
  /// @param dest destination quaternion
  /// @return the destination quaternion for chaining
  public static Quaternionf getCameraRotationQuat(Camera camera, Quaternionf dest) {
    return dest.set(camera.rotation());
  }

  /// Sets camera position in world coordinates.
  ///
  /// @param camera the camera to modify
  /// @param position new position
  public static void setCameraPosition(Camera camera, Vector3dc position) {
    // Apply the custom spatial position to the camera.
    CameraAccessor.of(camera).invokeSetPosition(position.x(), position.y(), position.z());
  }

  /// Sets camera rotation from a quaternion.
  ///
  /// Updates all internal camera state including euler angles, quaternion, and direction vectors.
  ///
  /// @param camera the camera to modify
  /// @param quat new rotation
  public static void setCameraRotationQuat(Camera camera, Quaternionfc quat) {
    CameraAccessor.of(camera).perspective_api$setRotation(quat);
  }

  /// Sets camera rotation from euler angles in degrees.
  ///
  /// @param camera the camera to modify
  /// @param xRot pitch in degrees
  /// @param yRot yaw in degrees
  public static void setCameraRotationEulerDeg(Camera camera, float xRot, float yRot) {
    CameraAccessor.of(camera).invokeSetRotation(yRot, xRot);
  }

  /// Sets camera rotation from euler angles in degrees.
  ///
  /// @param camera the camera to modify
  /// @param eulerDeg pitch and yaw in degrees
  public static void setCameraRotationEulerDeg(Camera camera, Vector2fc eulerDeg) {
    setCameraRotationEulerDeg(camera, eulerDeg.x(), eulerDeg.y());
  }

  /// Sets camera rotation from euler angles in degrees.
  ///
  /// @param camera the camera to modify
  /// @param eulerDeg pitch and yaw in degrees
  public static void setCameraRotationEulerDeg(Camera camera, Vec2 eulerDeg) {
    setCameraRotationEulerDeg(camera, eulerDeg.x, eulerDeg.y);
  }
}
