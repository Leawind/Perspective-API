package io.github.leawind.perspectiveapi.internal.bridge.events.context;

import net.minecraft.client.Camera;

public class CameraSetupContext {
  public Camera camera;
  public float partialTicks;

  public boolean cancelDefault;

  public CameraSetupContext() {}

  public void setup(Camera camera, float partialTicks) {
    this.camera = camera;
    this.partialTicks = partialTicks;
    this.cancelDefault = false;
  }

  public void cancelDefault() {
    this.cancelDefault = true;
  }
}
