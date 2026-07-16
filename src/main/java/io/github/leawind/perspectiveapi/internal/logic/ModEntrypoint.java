package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.spi.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public final class ModEntrypoint {
  private ModEntrypoint() {}

  public static void initialize() {
    PerspectiveRegistryImpl.INSTANCE.discoverAndRegister();

    StreamSupport.stream(ServiceLoader.load(PerspectiveSwitcherBehavior.class).spliterator(), false)
        .forEach(switcher -> PerspectiveManager.INSTANCE.switchers().register(switcher));

    ModEvents.register();
  }
}
