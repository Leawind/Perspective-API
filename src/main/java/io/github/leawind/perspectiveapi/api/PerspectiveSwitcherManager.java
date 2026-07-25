package io.github.leawind.perspectiveapi.api;

import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Exposes the player's selected perspective switcher for configuration and persistence.
///
/// Switcher selection is a player preference. Extensions should provide switchers through SPI,
/// rather than change this selection to coordinate their own behavior.
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface PerspectiveSwitcherManager {
  /// Returns an unmodifiable snapshot of switchers available to the player.
  @NonNull List<@NonNull PerspectiveSwitcher> getAvailableSwitchers();

  /// Returns the switcher selected by the player.
  @NonNull PerspectiveSwitcher getSelectedSwitcher();

  /// Sets the player's selected switcher.
  ///
  /// This method is intended for applying persisted state and for configuration UI. The switcher
  /// must be one returned by {@link #getAvailableSwitchers()}.
  ///
  /// @param switcher the player-selected switcher
  /// @throws IllegalArgumentException if the switcher is not registered
  void setSelectedSwitcher(@NonNull PerspectiveSwitcher switcher);
}
