package io.github.leawind.perspectiveapi.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

@ApiStatus.Internal
final class InitializationCoordinator {
  private record ReadyAction(@NonNull String key, @NonNull Runnable action) {}

  private boolean ready;
  private List<ReadyAction> pendingActions = new ArrayList<>();

  void runWhenReady(@NonNull String key, @NonNull Runnable action) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(action);
    synchronized (this) {
      if (!ready) {
        pendingActions.add(new ReadyAction(key, action));
        return;
      }
    }
    runAction(new ReadyAction(key, action));
  }

  void finish() {
    List<ReadyAction> actions;
    synchronized (this) {
      if (ready) return;
      ready = true;
      actions = pendingActions;
      pendingActions = new ArrayList<>();
    }

    RuntimeException failure = null;
    for (ReadyAction action : actions) {
      try {
        runAction(action);
      } catch (RuntimeException exception) {
        if (failure == null) {
          failure =
              new IllegalStateException(
                  "One or more Perspective API initialization actions failed");
        }
        failure.addSuppressed(exception);
      }
    }
    if (failure != null) throw failure;
  }

  @SuppressWarnings("removal")
  private static void runAction(@NonNull ReadyAction readyAction) {
    try {
      readyAction.action().run();
    } catch (Throwable throwable) {
      if (throwable instanceof VirtualMachineError error) throw error;
      if (throwable instanceof ThreadDeath error) throw error;
      throw new IllegalStateException(
          "Perspective API initialization action '" + readyAction.key() + "' failed", throwable);
    }
  }
}
