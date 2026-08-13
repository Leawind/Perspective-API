package io.github.leawind.perspectiveapi.api;

import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Runtime services installed by the internal implementation during mod initialization.
@ApiStatus.Internal
public final class PerspectiveAPIRuntime {
  private PerspectiveAPIRuntime() {}

  private static volatile @Nullable Services runtime;
  private static final Object RUNTIME_LOCK = new Object();
  private static final InitializationCoordinator INITIALIZATION = new InitializationCoordinator();

  /// Runtime services provided by the internal implementation.
  @ApiStatus.Internal
  public interface Services {
    @NonNull PerspectiveRegistry registry();

    @NonNull Transition transition();

    @NonNull PerspectiveModifierChain modifiers();

    @NonNull PerspectiveOverrideChain overrides();

    @NonNull PerspectiveSwitcherManager switchers();

    @NonNull PerspectiveSelection selection();

    @Nullable Perspective current();

    @Nullable PerspectiveState previousCameraState();

    boolean isCurrent(@NonNull String id);

    void onEnabledChanged(boolean enabled);
  }

  /// Installs the internal runtime implementation.
  @ApiStatus.Internal
  public static void install(@NonNull Services services) {
    Objects.requireNonNull(services);
    synchronized (RUNTIME_LOCK) {
      Services existing = PerspectiveAPIRuntime.runtime;
      if (existing != null) {
        if (existing.getClass() != services.getClass()) {
          throw new IllegalStateException("Perspective API runtime is already installed");
        }
        return;
      }
      PerspectiveAPIRuntime.runtime = services;
    }
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

  static void runWhenReady(@NonNull String key, @NonNull Runnable action) {
    INITIALIZATION.runWhenReady(key, action);
  }

  static @NonNull Services require() {
    Services runtime = PerspectiveAPIRuntime.runtime;
    if (runtime == null) {
      throw new IllegalStateException("Perspective API runtime is not initialized");
    }
    return runtime;
  }

  static @Nullable Services installed() {
    return runtime;
  }
}
