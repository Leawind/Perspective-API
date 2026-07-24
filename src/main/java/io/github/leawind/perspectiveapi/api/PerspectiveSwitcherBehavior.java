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
@ApiStatus.Experimental
@ApiStatus.OverrideOnly
public interface PerspectiveSwitcherBehavior extends PerspectiveSwitcher {

  /// Called once when the switcher is registered and initialized.
  default void init() {}

  /// Called when the list of switchable perspectives changes.
  ///
  /// @param switchables the updated list of available perspectives
  void onUpdateSwitchables(@NonNull List<@NonNull Perspective> switchables);

  /// Called when this switcher becomes the active switcher.
  ///
  /// @param currentPerspective the perspective active at the time of switch
  void onActivated(@NonNull Perspective currentPerspective);

  /// Called every client tick while this switcher is active.
  void clientTickWhenActive(@NonNull Minecraft minecraft);

  /// Called when this switcher is deactivated in favor of another switcher.
  void onDeactivated();

  /// Returns the ID of the currently selected perspective, or `null` if none.
  @Nullable String getSelected();
}
