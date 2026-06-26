package io.github.leawind.perspectiveapi.internal.bridge.access;

import io.github.leawind.perspectiveapi.internal.bridge.mixin.CameraAccessorMixin;
import net.minecraft.client.Camera;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/// Accessor interface for {@link net.minecraft.client.Camera} internals.
///
/// Provides access to private fields and methods via Mixin.
public interface CameraAccessor extends CameraAccessorMixin {

  /// Wraps a camera instance with accessor capabilities.
  ///
  /// @param camera the camera to wrap
  /// @return accessor interface for the camera
  static CameraAccessor of(Camera camera) {
    return (CameraAccessor) camera;
  }

  /// @return the forward direction vector
  Vector3f perspective_api$forwards();

  /// @return the up direction vector
  Vector3f perspective_api$up();

  /// @return the left direction vector
  Vector3f perspective_api$left();
  
  /// Updates all internal camera state including euler angles, quaternion, and direction vectors.
  void perspective_api$setRotation(Quaternionfc quat);
}
