package io.github.leawind.perspectiveapi.internal.logic.builtin.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.joml.Vector2d;
import org.junit.jupiter.api.Test;

class OrbitMenuTest {
  @Test
  void topSlotIsStableAcrossAngleSeam() {
    assertEquals(0, OrbitMenu.nearestWheelSlot(0.05, 3));
    assertEquals(0, OrbitMenu.nearestWheelSlot(-0.05, 3));
    assertEquals(0, OrbitMenu.nearestWheelSlot(Math.PI * 2 - 0.05, 3));
    assertEquals(
        List.of("top", "lower-left", "lower-right"),
        OrbitMenu.moveToNearestWheelSlot(
            List.of("top", "lower-left", "lower-right"), "top", -0.05));
  }

  @Test
  void selectsNearestWheelSlot() {
    assertEquals(1, OrbitMenu.nearestWheelSlot(Math.PI * 2 / 3, 3));
    assertEquals(2, OrbitMenu.nearestWheelSlot(Math.PI * 4 / 3, 3));
  }

  @Test
  void distinguishesClicksFromDragsByDistanceOnly() {
    assertTrue(OrbitMenu.isShortDrag(0, 0));
    assertTrue(OrbitMenu.isShortDrag(4, 0));
    assertTrue(OrbitMenu.isShortDrag(2.4, 3.2));
    assertFalse(OrbitMenu.isShortDrag(4.01, 0));
  }

  @Test
  void draggingPreservesTheGrabOffset() {
    PerspectiveActor actor = new PerspectiveActor("test");
    actor.body().position().set(0.5, -0.25);

    actor.beginDrag(new Vector2d(0.25, -0.5));
    actor.dragTo(new Vector2d(-0.25, 0.5));

    assertEquals(new Vector2d(0, 0.75), actor.body().position());
  }

  @Test
  void candidateGridExpandsFromTheWheelTowardTheLeftEdge() {
    double horizontalLimit = 8.0 / 9;
    double verticalLimit = 0.5;
    Vector2d target = new Vector2d();

    OrbitMenu.candidateGridTarget(0, 45, horizontalLimit, verticalLimit, target);
    assertTarget(target, -0.4, -0.4);

    OrbitMenu.candidateGridTarget(8, 45, horizontalLimit, verticalLimit, target);
    assertTarget(target, -0.4, 0.4);

    OrbitMenu.candidateGridTarget(9, 45, horizontalLimit, verticalLimit, target);
    assertTarget(target, -0.5, -0.4);

    OrbitMenu.candidateGridTarget(44, 45, horizontalLimit, verticalLimit, target);
    assertTarget(target, -0.8, 0.4);
  }

  @Test
  void candidateGridBalancesPartiallyFilledColumns() {
    Vector2d target = new Vector2d();

    OrbitMenu.candidateGridTarget(0, 10, 8.0 / 9, 0.5, target);
    assertTarget(target, -0.4, -0.2);

    OrbitMenu.candidateGridTarget(5, 10, 8.0 / 9, 0.5, target);
    assertTarget(target, -0.5, -0.2);

    OrbitMenu.candidateGridTarget(9, 10, 8.0 / 9, 0.5, target);
    assertTarget(target, -0.5, 0.2);
  }

  private static void assertTarget(Vector2d actual, double expectedX, double expectedY) {
    assertEquals(expectedX, actual.x, 1e-9);
    assertEquals(expectedY, actual.y, 1e-9);
  }
}
