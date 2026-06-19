package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;

public final class PerspectiveAPI {
  private PerspectiveAPI() {}

  public static final String MOD_ID = "perspective_api";
  public static final String MOD_NAME = "Perspective API";

  public static PerspectiveManager getManager() {
    return PerspectiveManagerImpl.INSTANCE;
  }
}
