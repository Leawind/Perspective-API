package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.spi.PerspectiveRegistrar;
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

    {
      PerspectiveAPI.getWheel()
          .register(VanillaPerspective.FIRST_PERSON.id(), 0)
          .register(VanillaPerspective.THIRD_PERSON_BACK.id(), 1)
          .register(VanillaPerspective.THIRD_PERSON_FRONT.id(), 2);
    }

    StreamSupport.stream(ServiceLoader.load(PerspectiveRegistrar.class).spliterator(), false)
        .forEach(registrar -> registrar.register(PerspectiveAPI.getRegistry()));
  }
}
