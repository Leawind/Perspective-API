package io.github.leawind.perspectiveapi.internal.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PerspectiveManagerTest {
  @AfterEach
  void resetLogicTickInterval() {
    PerspectiveManager.setLogicTickInterval(PerspectiveManager.DEFAULT_LOGIC_TICK_INTERVAL);
  }

  @Test
  void logicTickIntervalAcceptsPositiveValues() {
    PerspectiveManager.setLogicTickInterval(1);
    assertEquals(1, PerspectiveManager.getLogicTickInterval());

    PerspectiveManager.setLogicTickInterval(100);
    assertEquals(100, PerspectiveManager.getLogicTickInterval());
  }

  @Test
  void logicTickIntervalRejectsUnsupportedValues() {
    assertThrows(IllegalArgumentException.class, () -> PerspectiveManager.setLogicTickInterval(0));
    assertThrows(IllegalArgumentException.class, () -> PerspectiveManager.setLogicTickInterval(-1));
  }
}
