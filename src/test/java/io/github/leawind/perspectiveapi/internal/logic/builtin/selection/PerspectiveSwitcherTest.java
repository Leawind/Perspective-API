package io.github.leawind.perspectiveapi.internal.logic.builtin.selection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PerspectiveSwitcherTest {

  @Test
  void menuIsDisabledByDefault() {
    assertFalse(PerspectiveSwitcher.INSTANCE.isMenuEnabled());
  }

  @Test
  void menuEnabledFlagRoundTripsThroughPersistedState() {
    PerspectiveSwitcher switcher = PerspectiveSwitcher.INSTANCE;
    boolean previous = switcher.isMenuEnabled();
    try {
      switcher.setMenuEnabled(true);
      assertTrue(switcher.isMenuEnabled());
      assertTrue(switcher.extractState().menuEnabled());

      switcher.applyState(
          new PerspectiveSwitcherState(
              List.of(), Set.of(), PerspectiveSwitcher.DEFAULT_HOLD_TICKS, false, false, false));
      assertFalse(switcher.isMenuEnabled());
      assertFalse(switcher.extractState().menuEnabled());
    } finally {
      switcher.setMenuEnabled(previous);
    }
  }

  @Test
  void disablingAnAlreadyDisabledMenuIsHarmless() {
    PerspectiveSwitcher switcher = PerspectiveSwitcher.INSTANCE;
    boolean previous = switcher.isMenuEnabled();
    try {
      switcher.setMenuEnabled(false);
      assertFalse(switcher.isMenuEnabled());
    } finally {
      switcher.setMenuEnabled(previous);
    }
  }
}
