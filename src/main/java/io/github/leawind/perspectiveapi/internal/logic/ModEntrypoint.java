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
    var manager = PerspectiveAPI.getManager();

    manager
        .registry()
        .register(VanillaPerspective.FIRST_PERSON)
        .register(VanillaPerspective.THIRD_PERSON_BACK)
        .register(VanillaPerspective.THIRD_PERSON_FRONT);

    {
      manager
          .cycler()
          .add(VanillaPerspective.FIRST_PERSON.id(), 0)
          .add(VanillaPerspective.THIRD_PERSON_BACK.id(), 1)
          .add(VanillaPerspective.THIRD_PERSON_FRONT.id(), 2);
      manager.cycler().setActiveId(VanillaPerspective.FIRST_PERSON.id());
    }

    StreamSupport.stream(ServiceLoader.load(PerspectiveRegistrar.class).spliterator(), false)
        .forEach(registrar -> registrar.register(manager));
  }
}
