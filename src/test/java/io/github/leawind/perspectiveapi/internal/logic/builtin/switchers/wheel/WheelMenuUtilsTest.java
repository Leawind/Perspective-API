package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WheelMenuUtilsTest {
  @Test
  void rejectsNonPositiveSectorCounts() {
    assertEquals(-1, WheelMenuUtils.getSectorIndex(0, 0.0, 0.0));
    assertEquals(-1, WheelMenuUtils.getSectorIndex(-1, 0.0, 0.0));
  }

  @Test
  void mapsSectorCentersAndBoundaries() {
    int sectors = 4;

    assertEquals(0, WheelMenuUtils.getSectorIndex(sectors, 0.0, 0.0));
    assertEquals(1, WheelMenuUtils.getSectorIndex(sectors, 0.0, Math.PI / 2.0));
    assertEquals(2, WheelMenuUtils.getSectorIndex(sectors, 0.0, Math.PI));
    assertEquals(3, WheelMenuUtils.getSectorIndex(sectors, 0.0, 3.0 * Math.PI / 2.0));
    assertEquals(1, WheelMenuUtils.getSectorIndex(sectors, 0.0, Math.PI / 4.0));
    assertEquals(0, WheelMenuUtils.getSectorIndex(sectors, 0.0, -Math.PI / 4.0));
  }

  @Test
  void normalizesAnglesAndAppliesRotationOffset() {
    assertEquals(0, WheelMenuUtils.getSectorIndex(6, 0.2, 0.2 + 4.0 * Math.PI));
    assertEquals(5, WheelMenuUtils.getSectorIndex(6, 0.2, 0.2 - Math.PI / 3.0));
  }
}
