package io.github.leawind.perspectiveapi.internal.utils;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.KeyMapping;
import org.jspecify.annotations.Nullable;

/// Tracks key state transitions across ticks for vanilla KeyMapping instances.
public final class KeyStateTracker {
  private static final Map<KeyMapping, KeyStateTracker> INSTANCES = new WeakHashMap<>();

  private final KeyMapping key;
  private int holdTicks = 20;

  private boolean wasDown;
  private int heldTicks;
  private boolean holdTriggered;

  private @Nullable Runnable onDownHandler;
  private @Nullable Runnable onPressHandler;
  private @Nullable Runnable onHoldHandler;

  private KeyStateTracker(KeyMapping key) {
    this.key = key;
  }

  /// Returns a cached tracker for the given key, creating one with the builder if absent.
  public static KeyStateTracker of(KeyMapping key, java.util.function.Consumer<Builder> builder) {
    return INSTANCES.computeIfAbsent(
        key,
        k -> {
          var tracker = new KeyStateTracker(k);
          builder.accept(tracker.new Builder());
          return tracker;
        });
  }

  /// Call this every tick.
  public void tick() {
    if (key.isDown()) {
      if (!wasDown) {
        wasDown = true;
        heldTicks = 0;
        holdTriggered = false;
        // Drain vanilla click count so handleKeybinds() won't also process this key.
        while (key.consumeClick()) {}
        run(onDownHandler);
      }
      heldTicks++;
      if (!holdTriggered && heldTicks >= holdTicks) {
        holdTriggered = true;
        run(onHoldHandler);
      }
    } else {
      if (wasDown && !holdTriggered) {
        run(onPressHandler);
      }
      wasDown = false;
      heldTicks = 0;
      holdTriggered = false;
    }
  }

  /// Returns the tracked key.
  public KeyMapping key() {
    return key;
  }

  /// Returns true if the key is currently held down.
  public boolean isDown() {
    return wasDown;
  }

  /// Returns true if the hold callback has been triggered for the current press.
  public boolean isHoldTriggered() {
    return holdTriggered;
  }

  private static void run(@Nullable Runnable handler) {
    if (handler != null) {
      handler.run();
    }
  }

  /// Builder for configuring a KeyStateTracker.
  public final class Builder {
    private Builder() {}

    /// Sets the number of ticks the key must be held to trigger the hold callback.
    public Builder setHoldTicks(int ticks) {
      KeyStateTracker.this.holdTicks = ticks;
      return this;
    }

    /// Sets the callback invoked when the key is first pressed down.
    public Builder onDown(Runnable handler) {
      KeyStateTracker.this.onDownHandler = handler;
      return this;
    }

    /// Sets the callback invoked when the key is released before the hold threshold.
    public Builder onPress(Runnable handler) {
      KeyStateTracker.this.onPressHandler = handler;
      return this;
    }

    /// Sets the callback invoked when the key is held past the threshold.
    public Builder onHold(Runnable handler) {
      KeyStateTracker.this.onHoldHandler = handler;
      return this;
    }
  }
}
