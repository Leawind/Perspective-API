package io.github.leawind.perspectiveapi.api;

import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// The main entry point for the Perspective API framework.
///
/// Perspective API invokes extension callbacks on the Minecraft client thread. Unless a method's
/// documentation says otherwise, callers must also interact with mutable API services on that
/// thread. Actions passed to {@link #runWhenReady(String, Runnable)} follow that method's threading
/// contract instead.
///
/// ## Extension callback failures
///
/// Perspective API isolates non-fatal failures thrown by extension behavior callbacks so one
/// extension cannot interrupt the camera pipeline or other extensions. Failures are logged with
/// throttling, and the affected callback uses the fallback documented by its interface. Fatal JVM
/// errors are rethrown.
///
/// Initialization is different: a failed runtime registration is rolled back and the failure is
/// propagated to its caller. A failed service-loaded provider is skipped and its failure is logged.
public final class PerspectiveAPI {
  private PerspectiveAPI() {}

  /// The ID of mod Perspective API
  public static final String MOD_ID = "perspective_api";

  /// The display name of Perspective API
  public static final String MOD_NAME = "Perspective API";

  private static volatile boolean enabled = true;

  /// Runs an action after Perspective API has completed initialization.
  ///
  /// If the API is already ready, the action runs synchronously before this method returns.
  /// Otherwise, it runs synchronously on the thread that completes API initialization. Callers must
  /// not assume a particular thread or rely on the execution order of actions registered by
  /// different threads.
  ///
  /// The action is invoked exactly once. If multiple queued actions fail, all actions are invoked
  /// before their failures are reported together.
  ///
  /// @param key an identifier used to attribute failures
  /// @param action the initialization action
  public static void runWhenReady(@NonNull String key, @NonNull Runnable action) {
    PerspectiveAPIRuntime.runWhenReady(key, action);
  }

  /// @return whether the mod Perspective API is currently enabled
  public static boolean isEnabled() {
    return enabled;
  }

  /// Enable or disable the mod Perspective API
  ///
  /// When disabled, the active perspective is deactivated and the mod stops modifying camera state.
  /// Registered perspectives, overrides, and modifiers are retained.
  public static void setEnabled(boolean enabled) {
    if (PerspectiveAPI.enabled == enabled) return;
    PerspectiveAPI.enabled = enabled;
    PerspectiveAPIRuntime.Services runtime = PerspectiveAPIRuntime.installed();
    if (runtime != null) runtime.onEnabledChanged(enabled);
  }

  /// Returns the global registry for perspectives.
  ///
  /// Use {@link PerspectiveRegistry#contains} to safely check availability.
  ///
  /// @throws IllegalStateException if called before the internal runtime is initialized
  public static @NonNull PerspectiveRegistry getRegistry() {
    return PerspectiveAPIRuntime.require().registry();
  }

  /// Returns the controller for smooth camera transitions.
  @ApiStatus.Experimental
  public static @NonNull Transition getTransition() {
    return PerspectiveAPIRuntime.require().transition();
  }

  /// Returns the chain of modifiers applied to the camera state
  public static @NonNull PerspectiveModifierChain getModifierChain() {
    return PerspectiveAPIRuntime.require().modifiers();
  }

  /// Returns the final state from the last completed main-camera update.
  ///
  /// The snapshot includes the active perspective, all modifiers, and perspective-switch transition
  /// interpolation. It is published only after the camera update and final-state callback have
  /// completed, so querying it from inside a camera-state callback still returns the preceding
  /// completed update.
  ///
  /// The returned snapshot is independent and can be retained by the caller. It remains available
  /// across level and dimension changes, but is invalidated when Perspective API is disabled.
  ///
  /// @return an independent read-only snapshot, or `null` before the first completed camera update
  ///   and while the API is disabled
  /// @throws IllegalStateException if called before the internal runtime is initialized
  @ApiStatus.Experimental
  public static @Nullable PerspectiveState getPreviousCameraState() {
    return PerspectiveAPIRuntime.require().previousCameraState();
  }

  /// Returns the priority-based chain for temporary camera overrides
  @ApiStatus.Experimental
  public static @NonNull PerspectiveOverrideChain getOverrideChain() {
    return PerspectiveAPIRuntime.require().overrides();
  }

  /// Returns the player's persistent perspective selection.
  @ApiStatus.Experimental
  public static @NonNull PerspectiveSelection getSelection() {
    return PerspectiveAPIRuntime.require().selection();
  }

  /// Returns the perspective that currently owns the base camera state.
  ///
  /// Returns `null` while Perspective API is disabled or before a perspective has been activated.
  /// Use {@link #isCurrent(String)} to check a specific ID without a null check.
  ///
  /// @throws IllegalStateException if called before SPI discovery completes during mod loading
  public static @Nullable Perspective getCurrent() throws IllegalStateException {
    return PerspectiveAPIRuntime.require().current();
  }

  /// Checks if the currently active perspective matches the given ID.
  ///
  /// Returns `false` while Perspective API is disabled, before a perspective has been activated, or
  /// before SPI discovery completes.
  ///
  /// @param id the perspective ID to check
  /// @return `true` if the current perspective has the given ID, `false` otherwise
  public static boolean isCurrent(@NonNull String id) {
    Objects.requireNonNull(id);
    PerspectiveAPIRuntime.Services runtime = PerspectiveAPIRuntime.installed();
    return runtime != null && runtime.isCurrent(id);
  }
}
