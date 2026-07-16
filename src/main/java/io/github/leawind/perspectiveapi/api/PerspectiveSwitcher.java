package io.github.leawind.perspectiveapi.api;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface PerspectiveSwitcher {

  @NonNull Component getNameComponent();

  default @Nullable Component getDescriptionComponent() {
    return null;
  }
}
