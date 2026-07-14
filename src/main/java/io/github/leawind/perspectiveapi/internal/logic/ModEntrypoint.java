package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.spi.PerspectiveRegistrar;
import io.github.leawind.perspectiveapi.api.spi.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.internal.logic.builtin.VanillaPerspective;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;

public final class ModEntrypoint {
  private ModEntrypoint() {}

  public static void initialize() {
    registerVanillaPerspectives();

    ModEvents.register();
  }

  private static void registerVanillaPerspectives() {

    PerspectiveAPI.getRegistry()
        .register(VanillaPerspective.FIRST_PERSON)
        .register(VanillaPerspective.THIRD_PERSON_BACK)
        .register(VanillaPerspective.THIRD_PERSON_FRONT);

    StreamSupport.stream(ServiceLoader.load(PerspectiveRegistrar.class).spliterator(), false)
        .forEach(registrar -> registrar.register(PerspectiveAPI.getRegistry()));

    StreamSupport.stream(ServiceLoader.load(PerspectiveSwitcher.class).spliterator(), false)
        .forEach(switcher -> PerspectiveManager.INSTANCE.switchers().register(switcher));
  }
}
