package io.github.leawind.perspectiveapi.internal.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LogicUpdateSchedulerTest {
  @Test
  void intervalOneUpdatesOncePerTick() {
    LogicUpdateScheduler scheduler = new LogicUpdateScheduler();

    assertTrue(scheduler.tick(1));
    assertTrue(scheduler.tick(1));
    assertTrue(scheduler.tick(1));
  }

  @Test
  void largerIntervalSkipsTicksBetweenUpdates() {
    LogicUpdateScheduler scheduler = new LogicUpdateScheduler();

    assertTrue(scheduler.tick(3));
    assertFalse(scheduler.tick(3));
    assertFalse(scheduler.tick(3));
    assertTrue(scheduler.tick(3));
  }

  @Test
  void shorterIntervalTakesEffectWithoutOldDelay() {
    LogicUpdateScheduler scheduler = new LogicUpdateScheduler();

    assertTrue(scheduler.tick(20));
    assertFalse(scheduler.tick(20));
    assertTrue(scheduler.tick(1));
  }

  @Test
  void resetMakesNextTickUpdateImmediately() {
    LogicUpdateScheduler scheduler = new LogicUpdateScheduler();

    assertTrue(scheduler.tick(20));
    assertFalse(scheduler.tick(20));
    scheduler.reset();
    assertTrue(scheduler.tick(20));
  }
}
