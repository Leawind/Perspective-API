package io.github.leawind.perspectiveapi.internal.bridge.access;

import net.minecraft.client.Camera;

/// Access to projection state attached to the main camera by Mixin.
public interface CameraProjectionAccessor {
  static CameraProjectionAccessor of(Camera camera) {
    return (CameraProjectionAccessor) camera;
  }

  boolean perspective_api$isOrthographic();
}
