package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveChangeListener;
import io.github.leawind.perspectiveapi.api.PerspectiveChangeListenerRegistration;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

/// Notifies {@link PerspectiveChangeListener}s about effective current-perspective changes.
///
/// Notifications are synchronous. Changes triggered from inside a listener are queued until the
/// current notification finishes. Listener failures are logged without stopping later listeners.
final class PerspectiveChangeNotifier {
  /// Lambda class names differ between JVM launches, so failures are attributed to this constant.
  private static final String LISTENER_DIAGNOSTIC_ID = "PerspectiveChangeListener";

  private static final ExtensionInvoker EXTENSIONS =
      new ExtensionInvoker(
          LoggerFactory.getLogger(PerspectiveChangeNotifier.class), "Current-change listener");

  private record Change(@Nullable Perspective from, @Nullable Perspective to) {}

  private final class Registration implements PerspectiveChangeListenerRegistration {
    private final PerspectiveChangeListener listener;

    private Registration(@NonNull PerspectiveChangeListener listener) {
      this.listener = listener;
    }

    @Override
    public boolean unregister() {
      return PerspectiveChangeNotifier.this.unregister(this);
    }
  }

  private List<Registration> listeners = List.of();
  private final ArrayDeque<Change> pendingChanges = new ArrayDeque<>();
  private boolean notifying;

  synchronized void notifyChanged(@Nullable Perspective from, @Nullable Perspective to) {
    pendingChanges.addLast(new Change(from, to));
    if (notifying) return;

    notifying = true;
    try {
      Change change;
      while ((change = pendingChanges.pollFirst()) != null) notifyListeners(change);
    } finally {
      notifying = false;
    }
  }

  synchronized @NonNull PerspectiveChangeListenerRegistration subscribe(
      @NonNull PerspectiveChangeListener listener) {
    Registration registration = new Registration(Objects.requireNonNull(listener));
    List<Registration> updated = new ArrayList<>(listeners);
    updated.add(registration);
    listeners = List.copyOf(updated);
    return registration;
  }

  private void notifyListeners(@NonNull Change change) {
    for (Registration registration : listeners) {
      EXTENSIONS.run(
          LISTENER_DIAGNOSTIC_ID,
          "onChanged",
          () -> registration.listener.onChanged(change.from(), change.to()));
    }
  }

  private synchronized boolean unregister(@NonNull Registration registration) {
    List<Registration> updated = new ArrayList<>(listeners);
    if (!updated.removeIf(entry -> entry == registration)) return false;
    listeners = List.copyOf(updated);
    return true;
  }
}
