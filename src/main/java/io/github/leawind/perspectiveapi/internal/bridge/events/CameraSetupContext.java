package io.github.leawind.perspectiveapi.internal.bridge.events;

import net.minecraft.client.Camera;

public class CameraSetupContext {
  public Camera camera;
  public float partialTicks;

  public CameraSetupContext() {}

  public void setup(Camera camera, float partialTicks) {
    this.camera = camera;
    this.partialTicks = partialTicks;
  }
}
