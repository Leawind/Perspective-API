package io.github.leawind.perspectiveapi.internal.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;

public final class Sanitizer {
  private Sanitizer() {}

  // region finite checks

  public static boolean isFinite(Vector3dc vec) {
    return isFinite(vec.x()) && isFinite(vec.y()) && isFinite(vec.z());
  }

  public static boolean isFinite(Quaternionfc quat) {
    return isFinite(quat.x()) && isFinite(quat.y()) && isFinite(quat.z()) && isFinite(quat.w());
  }

  public static boolean isFinite(float f) {
    return Float.isFinite(f);
  }

  public static boolean isFinite(double d) {
    return Double.isFinite(d);
  }

  // endregion

  /// Executes an action at most once per cooldown period per key.
  public static final class ThrottledAction {
    private final long cooldownMs;
    private final Map<String, Long> lastRunTimes = new ConcurrentHashMap<>();

    public ThrottledAction(long cooldownMs) {
      this.cooldownMs = cooldownMs;
    }

    /// Runs the action if enough time has passed since the last run for this key.
    public void run(String key, Runnable action) {
      long now = System.currentTimeMillis();
      Long last = lastRunTimes.get(key);
      if (last != null && now - last < cooldownMs) {
        return;
      }
      lastRunTimes.put(key, now);
      action.run();
    }
  }
}
