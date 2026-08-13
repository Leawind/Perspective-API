package io.github.leawind.perspectiveapi.api;

import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Defines the behavior for a perspective switcher.
///
/// Implementations are discovered via {@link java.util.ServiceLoader} and managed by the {@link
/// io.github.leawind.perspectiveapi.api.PerspectiveSwitcherManager}.
///
/// A switcher reads and writes the shared selection through {@link PerspectiveAPI#getSelection()}.
/// It is responsible for responding to player interaction only while it is the selected switcher.
///
/// A provider whose ID is already registered is rejected without replacing the existing switcher.
@ApiStatus.Experimental
@ApiStatus.OverrideOnly
public interface PerspectiveSwitcherBehavior extends PerspectiveSwitcher {

  /// Called once when the switcher is registered and initialized. A failed service-loaded provider
  /// is skipped and its failure is logged.
  default void init() {}

  /// Called when the registered list of switchable perspectives changes.
  ///
  /// The list contains every registered perspective whose {@link PerspectiveInfo#switchable()}
  /// value is `true`, including perspectives that are currently unavailable. A switcher should
  /// consult {@link Perspective#isAvailable()} when it needs to decide whether a perspective can be
  /// selected. A failure is logged and otherwise ignored for that notification.
  ///
  /// @param switchablePerspectives the updated list of switchable perspectives
  void onSwitchablePerspectivesUpdated(@NonNull List<@NonNull Perspective> switchablePerspectives);

  /// Called when this switcher becomes the active switcher. A failure is logged and otherwise
  /// ignored.
  default void onActivated() {}

  /// Called when this switcher is deactivated in favor of another switcher. A failure is logged and
  /// otherwise ignored.
  default void onDeactivated() {}
}
