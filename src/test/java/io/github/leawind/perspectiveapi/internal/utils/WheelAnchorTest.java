package io.github.leawind.perspectiveapi.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WheelAnchorTest {
  private static final float DELTA = 1.0e-5f;

  @Test
  void movementInsideRadiusAccumulatesWithoutClamping() {
    WheelAnchor anchor = new WheelAnchor(10.0f);

    assertSame(anchor, anchor.moveBy(3.0f, 4.0f));

    assertEquals(3.0f, anchor.getOffsetX(), DELTA);
    assertEquals(4.0f, anchor.getOffsetY(), DELTA);
    assertFalse(anchor.isOnBorder());
  }

  @Test
  void movementOutsideRadiusClampsWhilePreservingDirection() {
    WheelAnchor anchor = new WheelAnchor(5.0f);

    anchor.moveBy(6.0f, 8.0f);

    assertEquals(3.0f, anchor.getOffsetX(), DELTA);
    assertEquals(4.0f, anchor.getOffsetY(), DELTA);
    assertTrue(anchor.isOnBorder());
  }

  @Test
  void resetClearsDisplacementAndBorderState() {
    WheelAnchor anchor = new WheelAnchor(1.0f);
    anchor.moveBy(2.0f, 0.0f);

    assertSame(anchor, anchor.reset());

    assertEquals(0.0f, anchor.getOffsetX());
    assertEquals(0.0f, anchor.getOffsetY());
    assertFalse(anchor.isOnBorder());
  }

  @Test
  void radiusCanBeChangedFluently() {
    WheelAnchor anchor = new WheelAnchor(2.0f);

    assertSame(anchor, anchor.setRadius(7.0f));
    assertEquals(7.0f, anchor.getRadius());
  }

  @Test
  void angleUsesClockwiseScreenCoordinates() {
    WheelAnchor anchor = new WheelAnchor(10.0f);

    anchor.moveBy(1.0f, 0.0f);
    assertEquals(0.0, anchor.getAngleRad(), 1.0e-12);

    anchor.reset().moveBy(0.0f, 1.0f);
    assertEquals(Math.PI / 2.0, anchor.getAngleRad(), 1.0e-12);
  }
}
