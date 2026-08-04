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

  /// Default number of client ticks between logic updates.
  @ApiStatus.Internal public static final int DEFAULT_LOGIC_TICK_INTERVAL = 1;

  private static volatile boolean enabled = true;
  private static volatile int logicTickInterval = DEFAULT_LOGIC_TICK_INTERVAL;
  private static volatile @Nullable Runtime runtime;
  private static final Object RUNTIME_LOCK = new Object();
  private static final InitializationCoordinator INITIALIZATION = new InitializationCoordinator();

  /// Runtime services installed by the internal implementation during mod initialization.
  @ApiStatus.Internal
  public interface Runtime {
    @NonNull PerspectiveRegistry registry();

    @NonNull Transition transition();

    @NonNull PerspectiveModifierChain modifiers();

    @NonNull PerspectiveOverrideChain overrides();

    @NonNull PerspectiveSwitcherManager switchers();

    @Nullable Perspective current();

    boolean isCurrent(@NonNull String id);

    void onEnabledChanged(boolean enabled);
  }

  /// Installs the internal runtime implementation.
  @ApiStatus.Internal
  public static void installRuntime(@NonNull Runtime runtime) {
    Objects.requireNonNull(runtime);
    synchronized (RUNTIME_LOCK) {
      Runtime existing = PerspectiveAPI.runtime;
      if (existing != null) {
        if (existing.getClass() != runtime.getClass()) {
          throw new IllegalStateException("Perspective API runtime is already installed");
        }
        return;
      }
      PerspectiveAPI.runtime = runtime;
    }
  }

  private static @NonNull Runtime requireRuntime() {
    Runtime runtime = PerspectiveAPI.runtime;
    if (runtime == null) {
      throw new IllegalStateException("Perspective API runtime is not initialized");
    }
    return runtime;
  }

  /// Runs an action after Perspective API has completed initialization.
  ///
  /// If the API is already ready, the action runs synchronously before this method returns.
  /// Otherwise, it runs synchronously on the thread that completes API initialization. Callers
  /// must not assume a particular thread or rely on the execution order of actions registered by
  /// different threads.
  ///
  /// The action is invoked exactly once. If multiple queued actions fail, all actions are invoked
  /// before their failures are reported together.
  ///
  /// @param key an identifier used to attribute failures
  /// @param action the initialization action
  public static void runWhenReady(@NonNull String key, @NonNull Runnable action) {
    INITIALIZATION.runWhenReady(key, action);
  }

  /// Marks Perspective API as ready and runs all queued initialization actions.
  @ApiStatus.Internal
  public static void finishInitialization() {
    if (runtime == null) {
      throw new IllegalStateException(
          "Cannot finish Perspective API initialization before installing its runtime");
    }
    INITIALIZATION.finish();
  }

  /// @return whether the mod Perspective API is currently enabled
  public static boolean isEnabled() {
    return enabled;
  }

  /// Enable or disable the mod Perspective API
  ///
  /// When disabled, the active perspective is deactivated and the mod stops modifying camera
  /// state. Registered perspectives, overrides, and modifiers are retained.
  public static void setEnabled(boolean enabled) {
    if (PerspectiveAPI.enabled == enabled) return;
    PerspectiveAPI.enabled = enabled;
    Runtime runtime = PerspectiveAPI.runtime;
    if (runtime != null) runtime.onEnabledChanged(enabled);
  }

  /// Returns the number of client ticks between Perspective API logic updates.
  @ApiStatus.Internal
  public static int getLogicTickInterval() {
    return logicTickInterval;
  }

  /// Sets the number of client ticks between Perspective API logic updates.
  ///
  /// @throws IllegalArgumentException if `logicTickInterval` is less than `1`
  @ApiStatus.Internal
  public static void setLogicTickInterval(int logicTickInterval) {
    if (logicTickInterval < 1) {
      throw new IllegalArgumentException("logicTickInterval must be at least 1");
    }
    PerspectiveAPI.logicTickInterval = logicTickInterval;
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

  /// Returns the perspective that currently owns the base camera state.
  ///
  /// Returns `null` while Perspective API is disabled or before a perspective has been activated.
  /// Use {@link #isCurrent(String)} to check a specific ID without a null check.
  ///
  /// @throws IllegalStateException if called before SPI discovery completes during mod loading
  public static @Nullable Perspective getCurrent() throws IllegalStateException {
    return requireRuntime().current();
  }

  /// Checks if the currently active perspective matches the given ID.
  ///
  /// Returns `false` while Perspective API is disabled, before a perspective has been activated,
  /// or before SPI discovery completes.
  ///
  /// @param id the perspective ID to check
  /// @return `true` if the current perspective has the given ID, `false` otherwise
  public static boolean isCurrent(@NonNull String id) {
    Objects.requireNonNull(id);
    Runtime runtime = PerspectiveAPI.runtime;
    return runtime != null && runtime.isCurrent(id);
  }
}
