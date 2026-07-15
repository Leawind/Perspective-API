package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import org.jspecify.annotations.NonNull;

public final class PerspectiveAPI {
  private PerspectiveAPI() {}

  public static final String MOD_ID = "perspective_api";
  public static final String MOD_NAME = "Perspective API";

  private static volatile boolean enabled = true;

  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(boolean enabled) {
    PerspectiveAPI.enabled = enabled;
  }

  public static PerspectiveRegistry getRegistry() {
    return PerspectiveRegistryImpl.INSTANCE;
  }

  public static @NonNull Transition getTransition() {
    return PerspectiveManager.INSTANCE.transition();
  }

  public static @NonNull PerspectiveModifierChain getModifierChain() {
    return PerspectiveManager.INSTANCE.modifiers();
  }

  public static @NonNull PerspectiveOverrideChain getOverrideChain() {
    return PerspectiveManager.INSTANCE.overrides();
  }

  public static @NonNull PerspectiveMeta getCurrent() {
    return PerspectiveManager.INSTANCE.getCurrent();
  }

  public static boolean isCurrent(@NonNull String id) {
    if (!PerspectiveRegistryImpl.INSTANCE.isDefaultFound()) {
      return false;
    }
    return PerspectiveManager.INSTANCE.getCurrent().id().equals(id);
  }
}
