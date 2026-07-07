package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;
import org.jspecify.annotations.NonNull;

/// Entry point for the Perspective API mod.
public final class PerspectiveAPI {
  private PerspectiveAPI() {}

  public static final String MOD_ID = "perspective_api";
  public static final String MOD_NAME = "Perspective API";

  private static volatile boolean enabled = true;

  /// Master switch that controls whether all mixins and event handlers are active.
  /// Set to `false` to completely revert to vanilla behavior.
  ///
  /// @see #setEnabled(boolean)
  public static boolean isEnabled() {
    return enabled;
  }

  /// @see #isEnabled()
  public static void setEnabled(boolean enabled) {
    PerspectiveAPI.enabled = enabled;
  }

  /// @return The global singleton of {@link PerspectiveManager}.
  public static @NonNull PerspectiveManager getManager() {
    return PerspectiveManagerImpl.INSTANCE;
  }
}
