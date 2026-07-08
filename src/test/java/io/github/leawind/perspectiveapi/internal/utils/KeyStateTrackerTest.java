package io.github.leawind.perspectiveapi.internal.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.KeyMapping;
import org.junit.jupiter.api.BeforeEach;
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

  @BeforeEach
  void clearCache() {}

  // ========== onDown ==========

  @Test
  void downFiresOnPress() {
    var key = new StubKey();
    var downFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.of(key, b -> b.setHoldTicks(HOLD_TICKS).onDown(() -> downFired.set(true)));

    key.setDown(true);
    key.simulateClick();
    tracker.tick();

    assertTrue(downFired.get());
  }

  @Test
  void downFiresExactlyOncePerPress() {
    var key = new StubKey();
    var count = new AtomicInteger();
    var tracker =
        KeyStateTracker.of(
            key, b -> b.setHoldTicks(HOLD_TICKS).onDown(() -> count.incrementAndGet()));

    key.setDown(true);
    key.simulateClick();
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
        KeyStateTracker.of(
            key, b -> b.setHoldTicks(HOLD_TICKS).onDown(() -> count.incrementAndGet()));

    key.setDown(true);
    key.simulateClick();
    tracker.tick();
    key.setDown(false);
    tracker.tick();

    key.setDown(true);
    key.simulateClick();
    tracker.tick();

    assertEquals(2, count.get());
  }

  // ========== consumeClick draining ==========

  @Test
  void tickDrainsClickCountOnPress() {
    var key = new StubKey();
    var tracker = KeyStateTracker.of(key, b -> b.setHoldTicks(HOLD_TICKS));

    key.setDown(true);
    key.simulateClick();
    key.simulateClick();
    tracker.tick();

    assertFalse(key.consumeClick(), "click count should be drained by tick");
  }

  @Test
  void tickDoesNotDrainClickWhenAlreadyDown() {
    var key = new StubKey();
    var tracker = KeyStateTracker.of(key, b -> b.setHoldTicks(HOLD_TICKS));

    key.setDown(true);
    key.simulateClick();
    tracker.tick();

    // Second tick while still down — should NOT drain again
    key.simulateClick();
    tracker.tick();

    assertTrue(key.consumeClick(), "click from second tick should remain");
  }

  // ========== onPress ==========

  @Test
  void pressFiresOnReleaseBeforeThreshold() {
    var key = new StubKey();
    var pressFired = new AtomicBoolean();
    var tracker =
        KeyStateTracker.of(
            key, b -> b.setHoldTicks(HOLD_TICKS).onPress(() -> pressFired.set(true)));

    key.setDown(true);
    key.simulateClick();
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
        KeyStateTracker.of(
            key,
            b ->
                b.setHoldTicks(HOLD_TICKS)
                    .onPress(() -> pressFired.set(true))
                    .onHold(() -> holdFired.set(true)));

    key.setDown(true);
    key.simulateClick();
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
        KeyStateTracker.of(
            key, b -> b.setHoldTicks(HOLD_TICKS).onPress(() -> count.incrementAndGet()));

    for (int i = 0; i < 3; i++) {
      key.setDown(true);
      key.simulateClick();
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
        KeyStateTracker.of(key, b -> b.setHoldTicks(HOLD_TICKS).onHold(() -> holdFired.set(true)));

    key.setDown(true);
    key.simulateClick();
    for (int i = 0; i < HOLD_TICKS; i++) {
      tracker.tick();
    }
    tracker.tick();

    assertTrue(holdFired.get());
  }

  @Test
  void holdFiresExactlyOnce() {
    var key = new StubKey();
    var count = new AtomicInteger();
    var tracker =
        KeyStateTracker.of(
            key, b -> b.setHoldTicks(HOLD_TICKS).onHold(() -> count.incrementAndGet()));

    key.setDown(true);
    key.simulateClick();
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
        KeyStateTracker.of(key, b -> b.setHoldTicks(HOLD_TICKS).onHold(() -> holdFired.set(true)));

    key.setDown(true);
    key.simulateClick();
    for (int i = 0; i < HOLD_TICKS - 1; i++) {
      tracker.tick();
    }
    key.setDown(false);
    tracker.tick();

    assertFalse(holdFired.get());
  }

  // ========== isDown / isHoldTriggered ==========

  @Test
  void isDownTracksKeyPressState() {
    var key = new StubKey();
    var tracker = KeyStateTracker.of(key, b -> b.setHoldTicks(HOLD_TICKS));

    assertFalse(tracker.isDown());

    key.setDown(true);
    key.simulateClick();
    tracker.tick();
    assertTrue(tracker.isDown());

    key.setDown(false);
    tracker.tick();
    assertFalse(tracker.isDown());
  }

  @Test
  void isHoldTriggeredResetsOnRelease() {
    var key = new StubKey();
    var tracker = KeyStateTracker.of(key, b -> b.setHoldTicks(HOLD_TICKS).onHold(() -> {}));

    key.setDown(true);
    key.simulateClick();
    for (int i = 0; i <= HOLD_TICKS; i++) {
      tracker.tick();
    }
    assertTrue(tracker.isHoldTriggered());

    key.setDown(false);
    tracker.tick();
    assertFalse(tracker.isHoldTriggered());
  }

  // ========== caching ==========

  @Test
  void ofReturnsSameInstanceForSameKey() {
    var key = new StubKey();
    var tracker1 = KeyStateTracker.of(key, b -> b.setHoldTicks(10));
    assertNotNull(tracker1);
    var tracker2 = KeyStateTracker.of(key, b -> b.setHoldTicks(20));

    assertSame(tracker1, tracker2);
  }

  // ========== mixed: hold then short press ==========

  @Test
  void holdThenShortPressWorks() {
    var key = new StubKey();
    var holdCount = new AtomicInteger();
    var pressCount = new AtomicInteger();
    var downCount = new AtomicInteger();
    var tracker =
        KeyStateTracker.of(
            key,
            b ->
                b.setHoldTicks(HOLD_TICKS)
                    .onDown(() -> downCount.incrementAndGet())
                    .onPress(() -> pressCount.incrementAndGet())
                    .onHold(() -> holdCount.incrementAndGet()));

    // first press: hold
    key.setDown(true);
    key.simulateClick();
    for (int i = 0; i <= HOLD_TICKS; i++) {
      tracker.tick();
    }
    key.setDown(false);
    tracker.tick();

    // second press: short
    key.setDown(true);
    key.simulateClick();
    tracker.tick();
    key.setDown(false);
    tracker.tick();

    assertEquals(1, holdCount.get());
    assertEquals(1, pressCount.get());
    assertEquals(2, downCount.get());
  }

  // ========== no callbacks ==========

  @Test
  void tickWithoutCallbacksDoesNotThrow() {
    var key = new StubKey();
    var tracker = KeyStateTracker.of(key, b -> b.setHoldTicks(HOLD_TICKS));

    key.setDown(true);
    key.simulateClick();
    tracker.tick();
    key.setDown(false);
    tracker.tick();
  }

  // ========== key accessor ==========

  @Test
  void keyReturnsTrackedKey() {
    var key = new StubKey();
    var tracker = KeyStateTracker.of(key, b -> b.setHoldTicks(HOLD_TICKS));

    assertSame(key, tracker.key());
  }
}
