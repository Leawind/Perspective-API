package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ToggleKeyGestureTest {
  @Test
  void releaseBeforeThresholdIsShortPress() {
    ToggleKeyGesture gesture = new ToggleKeyGesture(3);
    AtomicInteger shortPresses = new AtomicInteger();
    AtomicInteger holds = new AtomicInteger();
    gesture.onShortPress(shortPresses::incrementAndGet);
    gesture.onHoldStart(holds::incrementAndGet);

    gesture.tick(true);
    gesture.tick(true);
    gesture.tick(false);

    assertEquals(1, shortPresses.get());
    assertEquals(0, holds.get());
  }

  @Test
  void reachingThresholdStartsAndStopsHold() {
    ToggleKeyGesture gesture = new ToggleKeyGesture(3);
    AtomicInteger shortPresses = new AtomicInteger();
    AtomicInteger holdStarts = new AtomicInteger();
    AtomicInteger holdStops = new AtomicInteger();
    gesture.onShortPress(shortPresses::incrementAndGet);
    gesture.onHoldStart(holdStarts::incrementAndGet);
    gesture.onHoldStop(holdStops::incrementAndGet);

    gesture.tick(true);
    gesture.tick(true);
    gesture.tick(true);
    gesture.tick(true);
    gesture.tick(false);

    assertEquals(0, shortPresses.get());
    assertEquals(1, holdStarts.get());
    assertEquals(1, holdStops.get());
  }
}
