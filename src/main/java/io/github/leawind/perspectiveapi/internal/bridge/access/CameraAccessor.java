package io.github.leawind.perspectiveapi.internal.bridge.access;

import io.github.leawind.perspectiveapi.internal.bridge.mixin.CameraAccessorMixin;
import net.minecraft.client.Camera;
import org.joml.Vector3f;

public interface CameraAccessor extends CameraAccessorMixin {
  static CameraAccessor of(Camera camera) {
    return (CameraAccessor) camera;
  }

  Vector3f perspective_api$forwards();

  Vector3f perspective_api$up();

  Vector3f perspective_api$left();
}
