package io.github.leawind.perspectiveapi.internal.bridge.events;

/// Mutable bridge context used to select the world's projection for the current render frame.
public final class ModifyProjectionContext {
  /// Whether the world should use an orthographic projection.
  public boolean orthographic;

  /// The vertical span of the orthographic view in world units.
  public float orthographicHeight;

  public void setup() {
    orthographic = false;
    orthographicHeight = 16.0f;
  }
}
