package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;
import org.jspecify.annotations.NonNull;

/// Entry point for mod Perspective API
public final class PerspectiveAPI {
  private PerspectiveAPI() {}

  public static final String MOD_ID = "perspective_api";
  public static final String MOD_NAME = "Perspective API";

  /// @return  the global singleton of {@link PerspectiveManager}
  public static @NonNull PerspectiveManager getManager() {
    return PerspectiveManagerImpl.INSTANCE;
  }
}
