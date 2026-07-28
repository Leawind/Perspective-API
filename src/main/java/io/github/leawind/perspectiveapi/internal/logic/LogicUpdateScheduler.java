package io.github.leawind.perspectiveapi.internal.logic;

final class LogicUpdateScheduler {
  private int ticksUntilUpdate;

  boolean tick(int intervalTicks) {
    ticksUntilUpdate = Math.min(ticksUntilUpdate, intervalTicks - 1);
    if (ticksUntilUpdate > 0) {
      ticksUntilUpdate--;
      return false;
    }

    ticksUntilUpdate = intervalTicks - 1;
    return true;
  }

  void reset() {
    ticksUntilUpdate = 0;
  }
}
