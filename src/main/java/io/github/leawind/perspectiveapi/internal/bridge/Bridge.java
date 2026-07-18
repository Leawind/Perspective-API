package io.github.leawind.perspectiveapi.internal.bridge;

import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2fc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class Bridge {
  private Bridge() {}
  
  public static @Nullable Screen getScreen(@NonNull Minecraft minecraft){
    /*? if >=26.2 {*/
    return minecraft.gui.screen();
    /*? } else {*/
    /*return minecraft.screen;
    *//*? }*/
  }

  /// Returns the current Minecraft data version number.
  public static int getDataVersion() {
    /*? if >=1.21.11 {*/
    return SharedConstants.getCurrentVersion().dataVersion().version();
    /*? } else {*/
    /*return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    *//*? }*/
  }

  public static Identifier parseIdentifier(String identifier) {
    /*? if >=1.21 {*/
    return Identifier.parse(identifier);
    /*? } else {*/
    /*return new Identifier(identifier);
    *//*? }*/
  }

  public static Identifier createIdentifier(String namespace, String path) {
    /*? if >=1.21 {*/
    return Identifier.fromNamespaceAndPath(namespace, path);
    /*? } else {*/
    /*return new Identifier(namespace, path);
    *//*? }*/
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
    // TODO This might cause screen flickering
    // /*var levelRenderer = minecraft.levelRenderer;
    // if (levelRenderer != null) {
    //   levelRenderer.needsUpdate();
    // }
    // *//*? }*/
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
    *//*? }*/
  }

  /// Opens a screen in the Minecraft client.
  public static void setScreen(Screen screen) {
    /*? if >=26.2 {*/
    Minecraft.getInstance().setScreenAndShow(screen);
    /*? } else {*/
    /*Minecraft.getInstance().setScreen(screen);
    *//*? }*/
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

  /// Gets camera rotation as a quaternion, follow API convention
  ///
  /// @param camera the camera to query
  /// @param dest destination quaternion
  /// @return the destination quaternion for chaining
  public static Quaternionf getCameraRotation(Camera camera, Quaternionf dest) {
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
  public static void setCameraRotation(Camera camera, Quaternionfc mcQuat) {
    CameraAccessor.of(camera).perspective_api$setRotation(mcQuat);
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
