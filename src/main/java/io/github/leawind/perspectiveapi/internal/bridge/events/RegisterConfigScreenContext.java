package io.github.leawind.perspectiveapi.internal.bridge.events;

import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

public class RegisterConfigScreenContext {
  private Function<Screen, Screen> builder = null;

  public void register(Function<Screen, Screen> builder) {
    this.builder = builder;
  }

  public @Nullable Function<Screen, Screen> getBuilder() {
    return builder;
  }
}
