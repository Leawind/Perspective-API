package io.github.leawind.perspectiveapi.internal.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import org.jspecify.annotations.Nullable;

/// Tracks key state transitions across ticks for vanilla KeyMapping instances.
public final class KeyStateTracker {
  private static final Map<String, KeyStateTracker> INSTANCES = new HashMap<>();

  private final KeyMapping keyMapping;
  private int holdTicks = 4;

  private boolean wasDown;
  private int heldTicks;
  private boolean holdTriggered;
  private boolean pressTriggered;

  private @Nullable Runnable onDownHandler;
  private @Nullable Runnable onUpHandler;
  private @Nullable Runnable onPressHandler;
  private @Nullable Runnable onHoldHandler;
  private @Nullable Runnable onHoldStopHandler;

  private KeyStateTracker(KeyMapping keyMapping) {
    this.keyMapping = keyMapping;
  }

  public static KeyStateTracker of(String key, KeyMapping keyMapping, Consumer<Builder> builder) {
    return INSTANCES.computeIfAbsent(
        key,
        ignored -> {
          var tracker = new KeyStateTracker(keyMapping);
          builder.accept(tracker.new Builder());
          return tracker;
        });
  }

  public Builder builder(KeyMapping keyMapping) {
    var tracker = new KeyStateTracker(keyMapping);
    return tracker.new Builder();
  }

  /// Call this every tick.
  public KeyStateTracker tick() {
    if (keyMapping.isDown()) {
      if (!wasDown) {
        wasDown = true;
        heldTicks = 0;
        holdTriggered = false;
        pressTriggered = false;
        trigger(onDownHandler);
      }
      heldTicks++;
      if (!holdTriggered && heldTicks >= holdTicks) {
        holdTriggered = true;
        trigger(onHoldHandler);
      }
    } else {
      if (wasDown) {
        if (!holdTriggered && !pressTriggered) {
          pressTriggered = true;
          trigger(onPressHandler);
        } else if (holdTriggered) {
          trigger(onHoldStopHandler);
        }
        trigger(onUpHandler);
      }
      wasDown = false;
      heldTicks = 0;
    }
    return this;
  }

  public KeyStateTracker setHoldTicks(int ticks) {
    this.holdTicks = ticks;
    return this;
  }

  public void drain() {
    while (keyMapping.consumeClick()) {}
  }

  /// Returns the tracked key.
  public KeyMapping key() {
    return keyMapping;
  }

  /// Returns true if the key is currently held down.
  public boolean isDown() {
    return wasDown;
  }

  /// Returns true if the hold callback has been triggered for the current press.
  public boolean isHoldTriggered() {
    return holdTriggered;
  }

  private static void trigger(@Nullable Runnable handler) {
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

    /// Sets the callback invoked when the key is released, regardless of hold duration.
    public Builder onUp(Runnable handler) {
      KeyStateTracker.this.onUpHandler = handler;
      return this;
    }

    /// Sets the callback invoked when the key is released before the hold threshold.
    public Builder onPress(Runnable handler) {
      KeyStateTracker.this.onPressHandler = handler;
      return this;
    }

    /// Sets the callback invoked when the key is held past the threshold.
    public Builder onHoldStart(Runnable handler) {
      KeyStateTracker.this.onHoldHandler = handler;
      return this;
    }

    /// Sets the callback invoked when a held key is released after the hold threshold was reached.
    public Builder onHoldStop(Runnable handler) {
      KeyStateTracker.this.onHoldStopHandler = handler;
      return this;
    }

    public KeyStateTracker build() {
      return KeyStateTracker.this;
    }
  }
}
