package io.github.leawind.perspectiveapi.internal.bridge;

import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyFieldOfViewContext;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class Bridge {
  private Bridge() {}

  /// Returns the current Minecraft data version number.
  public static int getDataVersion() {
    /*? if >=1.21.11 {*/
    return SharedConstants.getCurrentVersion().dataVersion().version();
    /*? } else {*/
    /*return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    *//*? }*/
  }

  public static Identifier createIdentifier(String path) {
    return createIdentifier("minecraft", path);
  }

  public static Identifier createIdentifier(String namespace, String path) {
    /*? if >=1.21 {*/
    return Identifier.fromNamespaceAndPath(namespace, path);
    /*? } else {*/
    /*return new Identifier(namespace, path);
     *//*? }*/
  }

  /// Returns the current field of view.
  public static float getFov() {
    return ModifyFieldOfViewContext.getLastFieldOfView();
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
    *//*? }*/
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
}
