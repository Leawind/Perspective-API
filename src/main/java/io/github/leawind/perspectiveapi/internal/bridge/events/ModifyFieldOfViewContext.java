package io.github.leawind.perspectiveapi.internal.bridge.events;

public class ModifyFieldOfViewContext {
  /// Modifying this field will change the field of view.
  public float fieldOfView;

  public void setup(float fieldOfView) {
    this.fieldOfView = fieldOfView;
  }
}
