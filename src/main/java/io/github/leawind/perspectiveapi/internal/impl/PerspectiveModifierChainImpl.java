package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import io.github.leawind.perspectiveapi.api.PerspectiveModifier;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierPhase;
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
    private final String id;
    private final PerspectiveModifierPhase phase;
    private final int priority;
    private final PerspectiveModifier modifier;

    private Registration(
        @NonNull String id,
        @NonNull PerspectiveModifierPhase phase,
        int priority,
        @NonNull PerspectiveModifier modifier) {
      this.id = id;
      this.phase = phase;
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
      @NonNull String id,
      @NonNull PerspectiveModifierPhase phase,
      int priority,
      @NonNull PerspectiveModifier modifier) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(phase);
    Objects.requireNonNull(modifier);
    if (id.isEmpty()) throw new IllegalArgumentException("Modifier id must not be empty");
    Registration registration = new Registration(id, phase, priority, modifier);
    synchronized (this) {
      if (entries.stream().anyMatch(entry -> entry.id.equals(id))) {
        throw new IllegalArgumentException("Modifier id is already registered: '" + id + "'");
      }
      List<Registration> newList = new ArrayList<>(entries);
      newList.add(registration);
      newList.sort(
          Comparator.comparing((Registration entry) -> entry.phase)
              .thenComparingInt(entry -> entry.priority));
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

  /// Applies active modifiers in one pipeline phase sequentially.
  ///
  /// Position, rotation, FOV, and orthographic height are validated after each modifier;
  /// invalid fields are individually reverted.
  public void applyCameraState(
      @NonNull PerspectiveModifierPhase phase,
      PerspectiveState.@NonNull Mutable state,
      @NonNull PerspectiveContext ctx) {
    Objects.requireNonNull(phase);
    PerspectiveStateImpl backup = new PerspectiveStateImpl();
    for (Registration entry : entries) {
      if (entry.phase != phase) continue;
      String id = entry.id;
      if (!EXTENSIONS.testOrElse(id, "isAvailable", entry.modifier::isAvailable, false)) continue;

      backup.set(state);
      if (!EXTENSIONS.run(id, "apply", () -> entry.modifier.apply(state, ctx))) {
        restore(state, backup);
        continue;
      }
      sanitizer.sanitize(
          "modifier." + phase + "." + id,
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
