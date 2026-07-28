package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import org.jspecify.annotations.Nullable;

final class ToggleKeyGesture {
  private final int holdTicks;
  private boolean down;
  private boolean held;
  private int ticks;
  private @Nullable Runnable onShortPress;
  private @Nullable Runnable onHoldStart;
  private @Nullable Runnable onHoldStop;

  ToggleKeyGesture(int holdTicks) {
    if (holdTicks <= 0) throw new IllegalArgumentException("holdTicks must be positive");
    this.holdTicks = holdTicks;
  }

  void onShortPress(@Nullable Runnable listener) {
    onShortPress = listener;
  }

  void onHoldStart(@Nullable Runnable listener) {
    onHoldStart = listener;
  }

  void onHoldStop(@Nullable Runnable listener) {
    onHoldStop = listener;
  }

  void tick(boolean isDown) {
    if (isDown) {
      if (!down) {
        down = true;
        held = false;
        ticks = 0;
      }
      ticks++;
      if (!held && ticks >= holdTicks) {
        held = true;
        run(onHoldStart);
      }
      return;
    }

    if (down) {
      if (held) run(onHoldStop);
      else run(onShortPress);
    }
    reset();
  }

  void reset() {
    down = false;
    held = false;
    ticks = 0;
  }

  private static void run(@Nullable Runnable listener) {
    if (listener != null) listener.run();
  }
}
