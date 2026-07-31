package io.github.leawind.perspectiveapi.api;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Represents a player-facing UI mechanism for selecting perspectives.
///
/// A switcher presents a list of switchable perspectives and provides a display name
/// (and optional description) for itself. The actual switching logic is defined by
/// {@link PerspectiveSwitcherBehavior}; this interface exposes only the metadata
/// visible to configuration and persistence systems. Extensions should not use it to change the
/// player's switcher selection.
///
/// @see PerspectiveSwitcherBehavior
/// @see PerspectiveSwitcherManager
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface PerspectiveSwitcher {

  /// Returns the non-empty stable ID used to identify this switcher in configuration and persisted
  /// state.
  @NonNull String id();

  /// Returns the display name of this switcher.
  @NonNull Component name();

  /// Returns the description of this switcher, or `null` if none.
  default @Nullable Component description() {
    return null;
  }
}
