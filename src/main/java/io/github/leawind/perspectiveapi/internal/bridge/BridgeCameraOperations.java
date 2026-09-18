package io.github.leawind.perspectiveapi.internal.bridge;

import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Production {@link CameraOperations} backed by the {@link Bridge} statics.
public final class BridgeCameraOperations implements CameraOperations {
  public static final BridgeCameraOperations INSTANCE = new BridgeCameraOperations();

  private BridgeCameraOperations() {}

  @Override
  public @Nullable Camera getMainCamera() {
    return Bridge.getMainCamera();
  }

  @Override
  public @Nullable Entity getCameraEntity(@NonNull Camera camera) {
    return CameraAccessor.of(camera).getEntity();
  }

  @Override
  public @NonNull Vector3d getCameraPosition(@NonNull Camera camera, @NonNull Vector3d dest) {
    return Bridge.getCameraPosition(camera, dest);
  }

  @Override
  public @NonNull Quaternionf getCameraRotation(
      @NonNull Camera camera, @NonNull Quaternionf dest) {
    return Bridge.getCameraRotation(camera, dest);
  }

  @Override
  public void setCameraPosition(@NonNull Camera camera, @NonNull Vector3dc position) {
    Bridge.setCameraPosition(camera, position);
  }

  @Override
  public void setCameraRotation(@NonNull Camera camera, @NonNull Quaternionfc rotation) {
    Bridge.setCameraRotation(camera, rotation);
  }

  @Override
  public void updateCameraType(@NonNull CameraType cameraType) {
    Bridge.updateCameraType(cameraType);
  }
}
