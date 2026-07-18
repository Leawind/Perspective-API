package io.github.leawind.perspectiveapi.api;

import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Manages the registered perspective switchers and the currently active switcher.
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface PerspectiveSwitcherManager {
  /// Returns a list of all registered switchers.
  @NonNull List<PerspectiveSwitcher> getSwitchers();

  /// Returns the currently active switcher.
  @NonNull PerspectiveSwitcher getSwitcher();

  /// Sets the active switcher.
  ///
  /// @param switcher the switcher to activate
  void setSwitcher(@NonNull PerspectiveSwitcher switcher);
}
