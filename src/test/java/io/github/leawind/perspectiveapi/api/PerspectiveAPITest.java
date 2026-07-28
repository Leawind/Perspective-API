package io.github.leawind.perspectiveapi.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PerspectiveAPITest {
  @AfterEach
  void resetLogicTickInterval() {
    PerspectiveAPI.setLogicTickInterval(PerspectiveAPI.DEFAULT_LOGIC_TICK_INTERVAL);
  }

  @Test
  void logicTickIntervalAcceptsPositiveValues() {
    PerspectiveAPI.setLogicTickInterval(1);
    assertEquals(1, PerspectiveAPI.getLogicTickInterval());

    PerspectiveAPI.setLogicTickInterval(100);
    assertEquals(100, PerspectiveAPI.getLogicTickInterval());
  }

  @Test
  void logicTickIntervalRejectsUnsupportedValues() {
    assertThrows(
        IllegalArgumentException.class, () -> PerspectiveAPI.setLogicTickInterval(0));
    assertThrows(
        IllegalArgumentException.class, () -> PerspectiveAPI.setLogicTickInterval(-1));
  }
}
