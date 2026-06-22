package io.github.leawind.perspectiveapi.internal.bridge;

import net.minecraft.client.Camera;
import org.joml.Vector3f;

public interface CameraAdapter {
  Vector3f perspective_api$accessForwards();

  Vector3f perspective_api$accessUp();

  Vector3f perspective_api$accessLeft();

  static CameraAdapter of(Camera camera) {
    return (CameraAdapter) camera;
  }
}
