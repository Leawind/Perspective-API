package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPIRuntime;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;

public final class ModEntrypoint {
  private ModEntrypoint() {}

  public static void initialize() {
    PerspectiveAPIRuntime.install(PerspectiveAPIRuntimeImpl.INSTANCE);
    PerspectiveRegistryImpl.INSTANCE.discoverAndRegister();
    ModEvents.register();
    PerspectiveAPIRuntime.finishInitialization();
  }
}
