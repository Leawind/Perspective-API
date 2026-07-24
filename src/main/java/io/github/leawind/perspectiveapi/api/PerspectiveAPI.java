package io.github.leawind.perspectiveapi.api;

import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// The main entry point for the Perspective API framework.
public final class PerspectiveAPI {
  private PerspectiveAPI() {}

  /// The ID of mod Perspective API
  public static final String MOD_ID = "perspective_api";

  /// The display name of Perspective API
  public static final String MOD_NAME = "Perspective API";

  private static volatile boolean enabled = true;
  private static volatile @Nullable Runtime runtime;

  /// Runtime services installed by the internal implementation during mod initialization.
  @ApiStatus.Internal
  public interface Runtime {
    @NonNull PerspectiveRegistry registry();

    @NonNull Transition transition();

    @NonNull PerspectiveModifierChain modifiers();

    @NonNull PerspectiveOverrideChain overrides();

    @NonNull PerspectiveSwitcherManager switchers();

    @NonNull Perspective current() throws IllegalStateException;

    boolean isCurrent(@NonNull String id);
  }

  /// Installs the internal runtime implementation.
  @ApiStatus.Internal
  public static void installRuntime(@NonNull Runtime runtime) {
    Objects.requireNonNull(runtime);
    Runtime existing = PerspectiveAPI.runtime;
    if (existing != null && existing.getClass() != runtime.getClass()) {
      throw new IllegalStateException("Perspective API runtime is already installed");
    }
    PerspectiveAPI.runtime = runtime;
  }

  private static @NonNull Runtime requireRuntime() {
    Runtime runtime = PerspectiveAPI.runtime;
    if (runtime == null) {
      throw new IllegalStateException("Perspective API runtime is not initialized");
    }
    return runtime;
  }

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
  /// Use {@link PerspectiveRegistry#contains} to safely check availability.
  ///
  /// @throws IllegalStateException if called before the internal runtime is initialized
  public static @NonNull PerspectiveRegistry getRegistry() {
    return requireRuntime().registry();
  }

  /// Returns the controller for smooth camera transitions.
  @ApiStatus.Experimental
  public static @NonNull Transition getTransition() {
    return requireRuntime().transition();
  }

  /// Returns the chain of modifiers applied to the camera state
  public static @NonNull PerspectiveModifierChain getModifierChain() {
    return requireRuntime().modifiers();
  }

  /// Returns the priority-based chain for temporary camera overrides
  @ApiStatus.Experimental
  public static @NonNull PerspectiveOverrideChain getOverrideChain() {
    return requireRuntime().overrides();
  }

  /// Returns the manager for perspective switchers
  @ApiStatus.Experimental
  public static @NonNull PerspectiveSwitcherManager getSwitcherManager() {
    return requireRuntime().switchers();
  }

  /// Returns the currently active perspective, or throws if mod is not initialized.
  ///
  /// Use {@link #isCurrent(String)} to check if the current perspective has specific id;
  ///
  /// @throws IllegalStateException if called before SPI discovery completes during mod loading
  public static @NonNull Perspective getCurrent() throws IllegalStateException {
    return requireRuntime().current();
  }

  /// Checks if the currently active perspective matches the given ID.
  ///
  /// Returns `false` if called before SPI discovery completes, unlike {@link #getCurrent()} which
  /// throws.
  ///
  /// @param id the perspective ID to check
  /// @return `true` if the current perspective has the given ID, `false` otherwise
  public static boolean isCurrent(@NonNull String id) {
    Objects.requireNonNull(id);
    Runtime runtime = PerspectiveAPI.runtime;
    return runtime != null && runtime.isCurrent(id);
  }
}
