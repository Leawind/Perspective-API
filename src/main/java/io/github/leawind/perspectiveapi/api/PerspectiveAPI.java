package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// The main entry point for the Perspective API framework.
public final class PerspectiveAPI {
  private PerspectiveAPI() {}

  /// The ID of mod Perspective API
  public static final String MOD_ID = "perspective_api";

  /// The display name of Perspective API
  public static final String MOD_NAME = "Perspective API";

  private static volatile boolean enabled = true;

  /// @return whether the mod Perspective API is currently enabled
  public static boolean isEnabled() {
    return enabled;
  }

  /// Enable or disable the mod Perspective API
  ///
  /// When disabled, the mod reverts to vanilla camera behavior
  public static void setEnabled(boolean enabled) {
    PerspectiveAPI.enabled = enabled;
  }

  /// Returns the global registry for perspectives.
  ///
  /// The returned registry may be empty if called before SPI discovery completes during mod
  /// loading.
  ///
  /// Use {@link PerspectiveRegistry#contains} to safely check availability.
  public static @NonNull PerspectiveRegistry getRegistry() {
    return PerspectiveRegistryImpl.INSTANCE;
  }

  /// Returns the controller for smooth camera transitions.
  @ApiStatus.Experimental
  public static @NonNull Transition getTransition() {
    return PerspectiveManager.INSTANCE.transition();
  }

  /// Returns the chain of modifiers applied to the camera state
  public static @NonNull PerspectiveModifierChain getModifierChain() {
    return PerspectiveManager.INSTANCE.modifiers();
  }

  /// Returns the priority-based chain for temporary camera overrides
  @ApiStatus.Experimental
  public static @NonNull PerspectiveOverrideChain getOverrideChain() {
    return PerspectiveManager.INSTANCE.overrides();
  }

  /// Returns the manager for perspective switchers
  @ApiStatus.Experimental
  public static @NonNull PerspectiveSwitcherManager getSwitcherManager() {
    return PerspectiveManager.INSTANCE.switchers();
  }

  /// Returns the currently active perspective, or throws if mod is not initialized.
  ///
  /// Use {@link #isCurrent(String)} to check if the current perspective has specific id;
  ///
  /// @throws IllegalStateException if called before SPI discovery completes during mod loading
  public static @NonNull Perspective getCurrent() throws IllegalStateException {
    return PerspectiveManager.INSTANCE.getCurrent();
  }

  /// Checks if the currently active perspective matches the given ID.
  ///
  /// Returns `false` if called before SPI discovery completes, unlike {@link #getCurrent()} which
  /// throws.
  ///
  /// @param id the perspective ID to check
  /// @return `true` if the current perspective has the given ID, `false` otherwise
  public static boolean isCurrent(@NonNull String id) {
    if (!PerspectiveRegistryImpl.INSTANCE.isDefaultFound()) {
      return false;
    }
    return PerspectiveManager.INSTANCE.getCurrent().id().equals(id);
  }
}
