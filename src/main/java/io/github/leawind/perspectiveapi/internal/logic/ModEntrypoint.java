package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import io.github.leawind.perspectiveapi.spi.PerspectiveSPI;

public final class ModEntrypoint {
  private ModEntrypoint() {}

  public static void initialize() {
    registerVanillaPerspectives();

    ModEvents.register();
  }

  private static void registerVanillaPerspectives() {
    var manager = PerspectiveManager.get();

    manager
        .registry()
        .register(VanillaPerspective.FIRST_PERSON)
        .register(VanillaPerspective.THIRD_PERSON_BACK)
        .register(VanillaPerspective.THIRD_PERSON_FRONT);

    manager
        .cycler()
        .add(VanillaPerspective.FIRST_PERSON.id(), 0)
        .add(VanillaPerspective.THIRD_PERSON_BACK.id(), 1)
        .add(VanillaPerspective.THIRD_PERSON_FRONT.id(), 2);

    manager.setActive(VanillaPerspective.FIRST_PERSON.id());

    PerspectiveSPI.load().forEach(registrar -> registrar.register(manager));
  }
}
