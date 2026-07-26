package io.github.leawind.perspectiveapi.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.Test;

class KeyStateTrackerTest {

  private static final int HOLD_TICKS = 5;

  private static class StubKey extends KeyMapping {
    private boolean down;
    private int clickCount;

    StubKey() {
      /*? if >=1.21.11 {*/
      super("test.key", 0, Category.MISC);
      /*? } else {*/
      /*super("test.key", 0, "key.categories.gameplay");
       */
      /*? }*/
    }

    public void setDown(boolean down) {
      this.down = down;
    }

    /// Simulates a GLFW key press event, incrementing click count.
    public void simulateClick() {
      clickCount++;
    }

    @Override
    public boolean isDown() {
      return down;
    }

    @Override
    public boolean consumeClick() {
      if (clickCount == 0) return false;
      clickCount--;
      return true;
    }
  }

  // ========== onDown ==========

  @Test
  void downFiresOnPress() {
    var key = new StubKey();
    var downFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onDown(() -> downFired.set(true))
            .build();

    key.setDown(true);
    tracker.tick();

    assertTrue(downFired.get());
  }

  @Test
  void downFiresExactlyOncePerPress() {
    var key = new StubKey();
    var count = new AtomicInteger();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onDown(() -> count.incrementAndGet())
            .build();

    key.setDown(true);
    for (int i = 0; i < 5; i++) {
      tracker.tick();
    }

    assertEquals(1, count.get());
  }

  @Test
  void downFiresAgainAfterRelease() {
    var key = new StubKey();
    var count = new AtomicInteger();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onDown(() -> count.incrementAndGet())
            .build();

    key.setDown(true);
    tracker.tick();
    key.setDown(false);
    tracker.tick();

    key.setDown(true);
    tracker.tick();

    assertEquals(2, count.get());
  }

  // ========== drain ==========

  @Test
  void tickDoesNotDrainClicks() {
    var key = new StubKey();
    var tracker = KeyStateTracker.builder(key).setHoldTicks(HOLD_TICKS).build();

    key.setDown(true);
    key.simulateClick();
    key.simulateClick();
    tracker.tick();

    assertTrue(key.consumeClick(), "tick no longer drains clicks");
    assertTrue(key.consumeClick());
    assertFalse(key.consumeClick());
  }

  @Test
  void drainRemovesClickCount() {
    var key = new StubKey();
    var tracker = KeyStateTracker.builder(key).setHoldTicks(HOLD_TICKS).build();

    key.setDown(true);
    key.simulateClick();
    key.simulateClick();
    tracker.drain();

    assertFalse(key.consumeClick(), "drain should remove all clicks");
  }

  // ========== onPress ==========

  @Test
  void pressFiresOnReleaseBeforeThreshold() {
    var key = new StubKey();
    var pressFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onPress(() -> pressFired.set(true))
            .build();

    key.setDown(true);
    tracker.tick();
    key.setDown(false);
    tracker.tick();

    assertTrue(pressFired.get());
  }

  @Test
  void pressNotFiredAfterHold() {
    var key = new StubKey();
    var pressFired = new AtomicBoolean();
    var holdFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onPress(() -> pressFired.set(true))
            .onHoldStart(() -> holdFired.set(true))
            .build();

    key.setDown(true);
    for (int i = 0; i <= HOLD_TICKS; i++) {
      tracker.tick();
    }
    key.setDown(false);
    tracker.tick();

    assertTrue(holdFired.get());
    assertFalse(pressFired.get());
  }

  @Test
  void pressFiresEachShortPress() {
    var key = new StubKey();
    var count = new AtomicInteger();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onPress(() -> count.incrementAndGet())
            .build();

    for (int i = 0; i < 3; i++) {
      key.setDown(true);
      tracker.tick();
      key.setDown(false);
      tracker.tick();
    }

    assertEquals(3, count.get());
  }

  // ========== onHold ==========

  @Test
  void holdFiresAfterThreshold() {
    var key = new StubKey();
    var holdFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onHoldStart(() -> holdFired.set(true))
            .build();

    key.setDown(true);
    for (int i = 0; i <= HOLD_TICKS; i++) {
      tracker.tick();
    }

    assertTrue(holdFired.get());
  }

  @Test
  void holdFiresExactlyAtThreshold() {
    var key = new StubKey();
    var holdFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onHoldStart(() -> holdFired.set(true))
            .build();

    key.setDown(true);
    for (int i = 0; i < HOLD_TICKS - 1; i++) {
      tracker.tick();
    }
    assertFalse(holdFired.get());

    tracker.tick();
    assertTrue(holdFired.get());
  }

  @Test
  void holdFiresExactlyOnce() {
    var key = new StubKey();
    var count = new AtomicInteger();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onHoldStart(() -> count.incrementAndGet())
            .build();

    key.setDown(true);
    for (int i = 0; i < HOLD_TICKS + 5; i++) {
      tracker.tick();
    }

    assertEquals(1, count.get());
  }

  @Test
  void holdNotFiredWhenReleasedBeforeThreshold() {
    var key = new StubKey();
    var holdFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onHoldStart(() -> holdFired.set(true))
            .build();

    key.setDown(true);
    for (int i = 0; i < HOLD_TICKS - 1; i++) {
      tracker.tick();
    }
    key.setDown(false);
    tracker.tick();

    assertFalse(holdFired.get());
  }

  // ========== onUp ==========

  @Test
  void upFiresOnShortPress() {
    var key = new StubKey();
    var upFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key).setHoldTicks(HOLD_TICKS).onUp(() -> upFired.set(true)).build();

    key.setDown(true);
    tracker.tick();
    key.setDown(false);
    tracker.tick();

    assertTrue(upFired.get());
  }

  @Test
  void upFiresAfterHold() {
    var key = new StubKey();
    var upFired = new AtomicBoolean();
    var holdFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onHoldStart(() -> holdFired.set(true))
            .onUp(() -> upFired.set(true))
            .build();

    key.setDown(true);
    for (int i = 0; i <= HOLD_TICKS; i++) {
      tracker.tick();
    }
    key.setDown(false);
    tracker.tick();

    assertTrue(holdFired.get());
    assertTrue(upFired.get());
  }

  @Test
  void upFiresEachTime() {
    var key = new StubKey();
    var count = new AtomicInteger();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onUp(() -> count.incrementAndGet())
            .build();

    for (int i = 0; i < 3; i++) {
      key.setDown(true);
      tracker.tick();
      key.setDown(false);
      tracker.tick();
    }

    assertEquals(3, count.get());
  }

  @Test
  void upNotFiredWhenKeyNeverPressed() {
    var key = new StubKey();
    var upFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key).setHoldTicks(HOLD_TICKS).onUp(() -> upFired.set(true)).build();

    tracker.tick();

    assertFalse(upFired.get());
  }

  // ========== onHoldStop ==========

  @Test
  void holdStopFiresAfterHold() {
    var key = new StubKey();
    var holdStopFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onHoldStop(() -> holdStopFired.set(true))
            .build();

    key.setDown(true);
    for (int i = 0; i <= HOLD_TICKS; i++) {
      tracker.tick();
    }
    key.setDown(false);
    tracker.tick();

    assertTrue(holdStopFired.get());
  }

  @Test
  void holdStopNotFiredOnShortPress() {
    var key = new StubKey();
    var holdStopFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onHoldStop(() -> holdStopFired.set(true))
            .build();

    key.setDown(true);
    tracker.tick();
    key.setDown(false);
    tracker.tick();

    assertFalse(holdStopFired.get());
  }

  @Test
  void holdStopFiresEachTime() {
    var key = new StubKey();
    var count = new AtomicInteger();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onHoldStop(() -> count.incrementAndGet())
            .build();

    for (int i = 0; i < 3; i++) {
      key.setDown(true);
      for (int j = 0; j <= HOLD_TICKS; j++) {
        tracker.tick();
      }
      key.setDown(false);
      tracker.tick();
    }

    assertEquals(3, count.get());
  }

  // ========== isDown / isHoldTriggered ==========

  @Test
  void isDownTracksKeyPressState() {
    var key = new StubKey();
    var tracker = KeyStateTracker.builder(key).setHoldTicks(HOLD_TICKS).build();

    assertFalse(tracker.isDown());

    key.setDown(true);
    tracker.tick();
    assertTrue(tracker.isDown());

    key.setDown(false);
    tracker.tick();
    assertFalse(tracker.isDown());
  }

  @Test
  void isHoldTriggeredResetsOnNextPress() {
    var key = new StubKey();
    var tracker =
        KeyStateTracker.builder(key).setHoldTicks(HOLD_TICKS).onHoldStart(() -> {}).build();

    key.setDown(true);
    for (int i = 0; i <= HOLD_TICKS; i++) {
      tracker.tick();
    }
    assertTrue(tracker.isHoldTriggered());

    key.setDown(false);
    tracker.tick();
    assertTrue(tracker.isHoldTriggered(), "still set after release");

    key.setDown(true);
    tracker.tick();
    assertFalse(tracker.isHoldTriggered(), "reset on new press");
  }

  // ========== mixed: hold then short press ==========

  @Test
  void holdThenShortPressWorks() {
    var key = new StubKey();
    var holdCount = new AtomicInteger();
    var pressCount = new AtomicInteger();
    var downCount = new AtomicInteger();
    var upCount = new AtomicInteger();
    var holdStopCount = new AtomicInteger();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(HOLD_TICKS)
            .onDown(downCount::incrementAndGet)
            .onPress(pressCount::incrementAndGet)
            .onHoldStart(holdCount::incrementAndGet)
            .onUp(upCount::incrementAndGet)
            .onHoldStop(holdStopCount::incrementAndGet)
            .build();

    // first press: hold
    key.setDown(true);
    for (int i = 0; i <= HOLD_TICKS; i++) {
      tracker.tick();
    }
    key.setDown(false);
    tracker.tick();

    // second press: short
    key.setDown(true);
    tracker.tick();
    key.setDown(false);
    tracker.tick();

    assertEquals(1, holdCount.get());
    assertEquals(1, pressCount.get());
    assertEquals(2, downCount.get());
    assertEquals(2, upCount.get());
    assertEquals(1, holdStopCount.get());
  }

  @Test
  void callbacksRunInLifecycleOrder() {
    var key = new StubKey();
    List<String> calls = new ArrayList<>();
    var tracker =
        KeyStateTracker.builder(key)
            .setHoldTicks(1)
            .onDown(() -> calls.add("down"))
            .onHoldStart(() -> calls.add("hold-start"))
            .onHoldStop(() -> calls.add("hold-stop"))
            .onUp(() -> calls.add("up"))
            .build();

    key.setDown(true);
    tracker.tick();
    key.setDown(false);
    tracker.tick();

    assertEquals(List.of("down", "hold-start", "hold-stop", "up"), calls);
  }

  // ========== no callbacks ==========

  @Test
  void tickWithoutCallbacksDoesNotThrow() {
    var key = new StubKey();
    var tracker = KeyStateTracker.builder(key).setHoldTicks(HOLD_TICKS).build();

    key.setDown(true);
    tracker.tick();
    key.setDown(false);
    tracker.tick();
  }

  // ========== key accessor ==========

  @Test
  void keyReturnsTrackedKey() {
    var key = new StubKey();
    var tracker = KeyStateTracker.builder(key).setHoldTicks(HOLD_TICKS).build();

    assertSame(key, tracker.key());
  }

  @Test
  void holdTickSettingCanBeReadAndChangedAfterBuild() {
    var key = new StubKey();
    var tracker = KeyStateTracker.builder(key).build();

    assertEquals(4, tracker.getHoldTicks());
    assertSame(tracker, tracker.setHoldTicks(9));
    assertEquals(9, tracker.getHoldTicks());
  }
}
