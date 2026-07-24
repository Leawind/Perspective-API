package io.github.leawind.perspectiveapi.internal.bridge.events;

public class ModifyFieldOfViewContext {
  /// Modifying this field will change the field of view.
  public float fieldOfViewDeg;

  public void setup(float fieldOfViewDeg) {
    this.fieldOfViewDeg = fieldOfViewDeg;
  }
}
