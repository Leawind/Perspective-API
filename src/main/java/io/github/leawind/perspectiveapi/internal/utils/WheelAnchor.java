package io.github.leawind.perspectiveapi.internal.utils;

/// Tracks the anchor point displacement for a radial wheel menu.
///
/// The anchor starts at the origin and moves with the cursor as a relative
/// displacement. It is clamped to a configurable radius so the direction from
/// the origin to the anchor always stays well-defined.
public class WheelAnchor {

  private float radius;

  private float offsetX = 0;
  private float offsetY = 0;

  private boolean isOnBorder = false;

  public WheelAnchor(float radius) {
    this.radius = radius;
  }

  public WheelAnchor setRadius(float radius) {
    this.radius = radius;
    return this;
  }

  public float getRadius() {
    return radius;
  }

  public float getOffsetX() {
    return offsetX;
  }

  public float getOffsetY() {
    return offsetY;
  }

  /// Moves the anchor by the given relative displacement and clamps it to the configured radius.
  public WheelAnchor moveBy(float dx, float dy) {
    offsetX += dx;
    offsetY += dy;

    float dist = (float) Math.sqrt(offsetX * offsetX + offsetY * offsetY);
    if (dist >= radius) {
      float scale = radius / dist;
      offsetX *= scale;
      offsetY *= scale;
      isOnBorder = true;
    } else {
      isOnBorder = false;
    }

    return this;
  }

  /// Resets the anchor to the origin.
  public WheelAnchor reset() {
    offsetX = 0;
    offsetY = 0;
    isOnBorder = false;
    return this;
  }

  public boolean isOnBorder() {
    return isOnBorder;
  }

  /// Returns the angle from the positive X axis to the anchor, in radians.
  ///
  /// This follows screen coordinates where 0 radians points right and positive angles go
  /// clockwise.
  public double getAngleRad() {
    return Math.atan2(offsetY, offsetX);
  }
}
