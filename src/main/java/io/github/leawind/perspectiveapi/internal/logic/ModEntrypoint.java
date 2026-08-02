package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.logic.debug.DebugErrorPerspective;
import io.github.leawind.perspectiveapi.internal.utils.Exceptions;
import io.github.leawind.perspectiveapi.platform.api.Services;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModEntrypoint {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModEntrypoint.class);

  private ModEntrypoint() {}

  public static void initialize() {
    PerspectiveAPI.installRuntime(PerspectiveAPIRuntimeImpl.INSTANCE);
    PerspectiveRegistryImpl.INSTANCE.discoverAndRegister();

    if (Services.PLATFORM_HELPER.isDevelopmentEnvironment()) {
      PerspectiveRegistryImpl.INSTANCE.register(
          DebugErrorPerspective.INFO, DebugErrorPerspective.INSTANCE);
    }

    var iterator = ServiceLoader.load(PerspectiveSwitcherBehavior.class).iterator();
    while (true) {
      PerspectiveSwitcherBehavior switcher;
      try {
        if (!iterator.hasNext()) break;
        switcher = iterator.next();
        PerspectiveManager.INSTANCE.switchers().register(switcher);
      } catch (Throwable throwable) {
        Exceptions.rethrowIfFatal(throwable);
        LOGGER.warn("Failed to load PerspectiveSwitcherBehavior implementation", throwable);
      }
    }

    ModEvents.register();
    PerspectiveAPI.finishInitialization();
  }
}
