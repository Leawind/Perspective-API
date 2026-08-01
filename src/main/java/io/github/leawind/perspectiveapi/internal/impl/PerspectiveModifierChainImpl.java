package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import io.github.leawind.perspectiveapi.api.PerspectiveModifier;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierRegistration;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.slf4j.LoggerFactory;

public final class PerspectiveModifierChainImpl implements PerspectiveModifierChain {
  private static final ExtensionInvoker EXTENSIONS =
      new ExtensionInvoker(LoggerFactory.getLogger(PerspectiveModifierChainImpl.class), "Modifier");

  private final class Registration implements PerspectiveModifierRegistration {
    private final int priority;
    private final PerspectiveModifier modifier;

    private Registration(int priority, @NonNull PerspectiveModifier modifier) {
      this.priority = priority;
      this.modifier = modifier;
    }

    @Override
    public boolean unregister() {
      return PerspectiveModifierChainImpl.this.unregister(this);
    }
  }

  private final ThrottledPerspectiveSanitizer sanitizer;

  private volatile List<Registration> entries = List.of();

  public PerspectiveModifierChainImpl(@NonNull ThrottledPerspectiveSanitizer sanitizer) {
    this.sanitizer = Objects.requireNonNull(sanitizer);
  }

  @Override
  public @NonNull PerspectiveModifierRegistration register(
      int priority, @NonNull PerspectiveModifier modifier) {
    Objects.requireNonNull(modifier);
    Registration registration = new Registration(priority, modifier);
    synchronized (this) {
      List<Registration> newList = new ArrayList<>(entries);
      newList.add(registration);
      newList.sort(Comparator.comparingInt(entry -> entry.priority));
      this.entries = List.copyOf(newList);
    }
    return registration;
  }

  private boolean unregister(@NonNull Registration registration) {
    synchronized (this) {
      List<Registration> newList = new ArrayList<>(entries);
      if (newList.removeIf(entry -> entry == registration)) {
        this.entries = List.copyOf(newList);
        return true;
      }
      return false;
    }
  }

  /// Applies all active modifiers' camera state transformations sequentially.
  ///
  /// Called after the base perspective establishes the target state,
  /// but before transition interpolation.
  ///
  /// Position, rotation, FOV, and orthographic height are validated after each modifier;
  /// invalid fields are individually reverted.
  public void applyCameraState(
      PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext ctx) {
    PerspectiveStateImpl backup = new PerspectiveStateImpl();
    for (Registration entry : entries) {
      String id = entry.modifier.getClass().getName();
      if (!EXTENSIONS.testOrElse(id, "isAvailable", entry.modifier::isAvailable, false)) continue;

      backup.set(state);
      if (!EXTENSIONS.run(id, "apply", () -> entry.modifier.apply(state, ctx))) {
        restore(state, backup);
        continue;
      }
      sanitizer.sanitize(
          "modifier." + id,
          state,
          backup,
          () -> "Modifier '" + id + "' produced invalid state. Reverting.");
    }
  }

  private static void restore(
      PerspectiveState.@NonNull Mutable target, @NonNull PerspectiveState source) {
    target.position().set(source.position());
    target.rotation().set(source.rotation());
    target.setFovDeg(source.getFovDeg());
    target.setProjectionMode(source.projectionMode());
    target.setOrthographicHeight(source.getOrthographicHeight());
  }
}
