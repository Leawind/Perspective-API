package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.logic.vanilla.VanillaFirstPersonPerspective;
import io.github.leawind.perspectiveapi.internal.logic.vanilla.VanillaThirdPersonPerspective;
import io.github.leawind.perspectiveapi.spi.PerspectiveSPI;

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
        .register(VanillaFirstPersonPerspective.INSTANCE)
        .register(VanillaThirdPersonPerspective.BACK)
        .register(VanillaThirdPersonPerspective.FRONT);

    {
      manager
          .cycler()
          .add(VanillaFirstPersonPerspective.INSTANCE.id(), 0)
          .add(VanillaThirdPersonPerspective.BACK.id(), 1)
          .add(VanillaThirdPersonPerspective.FRONT.id(), 2);
      manager.cycler().setActive(VanillaFirstPersonPerspective.INSTANCE.id());
    }

    PerspectiveSPI.load().forEach(registrar -> registrar.register(manager));
  }
}
