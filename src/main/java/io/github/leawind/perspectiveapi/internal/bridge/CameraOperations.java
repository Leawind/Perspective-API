package io.github.leawind.perspectiveapi.internal.bridge;

import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Injectable facade over the camera I/O helpers used by the manager layer.
///
/// Each method mirrors the corresponding {@link Bridge} static. Rotations use the vanilla
/// Minecraft convention; callers convert between conventions with {@link CameraSpace}.
public interface CameraOperations {
  /// Returns the main camera, or null if unavailable.
  @Nullable Camera getMainCamera();

  /// Returns the entity the camera is aligned with, or null when the camera has none.
  @Nullable Entity getCameraEntity(@NonNull Camera camera);

  /// Gets camera position in world coordinates into `dest`.
  @NonNull Vector3d getCameraPosition(@NonNull Camera camera, @NonNull Vector3d dest);

  /// Gets camera rotation in the vanilla convention into `dest`.
  @NonNull Quaternionf getCameraRotation(@NonNull Camera camera, @NonNull Quaternionf dest);

  /// Sets camera position in world coordinates.
  void setCameraPosition(@NonNull Camera camera, @NonNull Vector3dc position);

  /// Sets camera rotation from a quaternion in the vanilla convention.
  void setCameraRotation(@NonNull Camera camera, @NonNull Quaternionfc rotation);

  /// Updates the vanilla camera type, keeping the current value when it did not change.
  void updateCameraType(@NonNull CameraType cameraType);
}
