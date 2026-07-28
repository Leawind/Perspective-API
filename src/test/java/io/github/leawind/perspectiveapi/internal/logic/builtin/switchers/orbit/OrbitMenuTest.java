package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
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
}
