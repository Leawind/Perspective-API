package io.github.leawind.perspectiveapi.internal.bridge.events;

public class ModifyFieldOfViewContext {

  private static volatile float lastFieldOfView = 70.0f;

  /// Modifying this field will change the field of view.
  public float fieldOfView;

  public static float getLastFieldOfView() {
    return lastFieldOfView;
  }

  public static void setLastFieldOfView(float fieldOfView) {
    lastFieldOfView = fieldOfView;
  }

  public void setup(float fieldOfView) {
    this.fieldOfView = fieldOfView;
  }
}
