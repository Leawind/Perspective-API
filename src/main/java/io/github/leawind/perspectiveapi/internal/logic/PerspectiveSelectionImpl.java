package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveSelection;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

final class PerspectiveSelectionImpl implements PerspectiveSelection {
  private static final ExtensionInvoker EXTENSIONS =
      new ExtensionInvoker(
          LoggerFactory.getLogger(PerspectiveSelectionImpl.class), "Selection listener");

  private record Change(@Nullable String selectedPerspectiveId) {}

  private final class Registration implements ListenerRegistration {
    private final Listener listener;

    private Registration(@NonNull Listener listener) {
      this.listener = listener;
    }

    @Override
    public boolean unregister() {
      return PerspectiveSelectionImpl.this.unregister(this);
    }
  }

  /// Selected perspective ID
  private @Nullable String selected;

  private List<Registration> listeners = List.of();
  private final ArrayDeque<Change> pendingChanges = new ArrayDeque<>();
  private boolean notifying;

  @Override
  public synchronized @Nullable String get() {
    return selected;
  }

  @Override
  public synchronized void set(@Nullable String perspectiveId) {
    if (Objects.equals(selected, perspectiveId)) return;
    selected = perspectiveId;
    pendingChanges.addLast(new Change(perspectiveId));
    if (notifying) return;

    notifying = true;
    try {
      Change change;
      while ((change = pendingChanges.pollFirst()) != null) notifyListeners(change);
    } finally {
      notifying = false;
    }
  }

  @Override
  public synchronized @NonNull ListenerRegistration onChanged(@NonNull Listener listener) {
    Registration registration = new Registration(Objects.requireNonNull(listener));
    List<Registration> updated = new ArrayList<>(listeners);
    updated.add(registration);
    listeners = List.copyOf(updated);
    return registration;
  }

  private void notifyListeners(@NonNull Change change) {
    for (Registration registration : listeners) {
      EXTENSIONS.run(
          registration.listener.getClass().getName(),
          "onChanged",
          () -> registration.listener.onChanged(change.selectedPerspectiveId()));
    }
  }

  private synchronized boolean unregister(@NonNull Registration registration) {
    List<Registration> updated = new ArrayList<>(listeners);
    if (!updated.removeIf(entry -> entry == registration)) return false;
    listeners = List.copyOf(updated);
    return true;
  }
}
