package io.github.leawind.perspectiveapi.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@ApiStatus.Experimental
public interface PerspectiveSwitcher {

  /// Returns the display name of this switcher.
  @NonNull Component getNameComponent();

  /// Returns the description of this switcher, or `null` if none.
  default @Nullable Component getDescriptionComponent() {
    return null;
  }
}
