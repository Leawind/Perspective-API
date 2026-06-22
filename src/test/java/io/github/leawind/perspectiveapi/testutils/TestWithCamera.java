package io.github.leawind.perspectiveapi.testutils;

import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import net.minecraft.client.Camera;
import org.junit.jupiter.api.BeforeEach;

public class TestWithCamera {
  protected Camera camera;
  protected CameraAccessor cameraAccessor;

  @BeforeEach
  void beforeEach() {
    camera = new Camera();
    cameraAccessor = CameraAccessor.of(camera);
  }
}
