package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.spi.PerspectiveRegistrar;
import io.github.leawind.perspectiveapi.internal.logic.builtin.VanillaFirstPersonPerspective;
import io.github.leawind.perspectiveapi.internal.logic.builtin.VanillaThirdPersonPerspective;
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

    StreamSupport.stream(ServiceLoader.load(PerspectiveRegistrar.class).spliterator(), false)
        .forEach(registrar -> registrar.register(manager));
  }
}
