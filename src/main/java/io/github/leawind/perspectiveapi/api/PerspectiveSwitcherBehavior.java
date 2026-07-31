package io.github.leawind.perspectiveapi.api;

import java.util.List;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Defines the behavior for a perspective switcher.
///
/// Implementations are discovered via {@link java.util.ServiceLoader} and managed
/// by the {@link io.github.leawind.perspectiveapi.api.PerspectiveSwitcherManager}.
///
/// A provider whose ID is already registered is rejected without replacing the existing switcher.
@ApiStatus.Experimental
@ApiStatus.OverrideOnly
public interface PerspectiveSwitcherBehavior extends PerspectiveSwitcher {

  /// Called once when the switcher is registered and initialized.
  /// A failed service-loaded provider is skipped and its failure is logged.
  default void init() {}

  /// Called when the registered list of switchable perspectives changes.
  ///
  /// The list contains every registered perspective whose {@link PerspectiveInfo#switchable()}
  /// value is `true`, including perspectives that are currently unavailable. A switcher should
  /// consult {@link Perspective#isAvailable()} when it needs to decide whether a perspective can be
  /// selected.
  /// A failure is logged and otherwise ignored for that notification.
  ///
  /// @param switchablePerspectives the updated list of switchable perspectives
  void onSwitchablePerspectivesUpdated(@NonNull List<@NonNull Perspective> switchablePerspectives);

  /// Called when this switcher becomes the active switcher.
  /// A failure is logged and otherwise ignored.
  ///
  /// @param currentPerspective the perspective active at the time of switch
  void onActivated(@NonNull Perspective currentPerspective);

  /// Called every client tick while this switcher is active.
  /// A failure is logged and otherwise ignored for that tick.
  void clientTickWhenActive(@NonNull Minecraft minecraft);

  /// Called when this switcher is deactivated in favor of another switcher.
  /// A failure is logged and otherwise ignored.
  void onDeactivated();

  /// Returns the ID of the currently selected perspective, or `null` if none.
  /// A failure is logged and treated as `null` for that update.
  @Nullable String getSelectedPerspectiveId();
}
