package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.spi.PerspectiveSwitcherBehavior;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Represents a UI mechanism that allows the player to cycle through available perspectives.
///
/// A switcher presents a list of switchable perspectives and provides a display name
/// (and optional description) for itself. The actual switching logic is defined by
/// {@link PerspectiveSwitcherBehavior}; this interface exposes only the metadata
/// visible to other parts of the system.
///
/// @see PerspectiveSwitcherBehavior
/// @see PerspectiveSwitcherManager
@ApiStatus.Experimental
public interface PerspectiveSwitcher {

  /// Returns the display name of this switcher.
  @NonNull Component getNameComponent();

  /// Returns the description of this switcher, or `null` if none.
  default @Nullable Component getDescriptionComponent() {
    return null;
  }
}
