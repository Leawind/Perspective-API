package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveModifier;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
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

  /// Represents a registered modifier with its priority and key.
  private record ModifierEntry(
      @NonNull String key, int priority, @NonNull PerspectiveModifier modifier) {
    static final Comparator<ModifierEntry> COMPARATOR =
        Comparator.comparingInt(ModifierEntry::priority);
  }

  private final ThrottledPerspectiveSanitizer sanitizer;

  private volatile List<ModifierEntry> entries = List.of();

  public PerspectiveModifierChainImpl(@NonNull ThrottledPerspectiveSanitizer sanitizer) {
    this.sanitizer = Objects.requireNonNull(sanitizer);
  }

  @Override
  public void register(@NonNull String key, int priority, @NonNull PerspectiveModifier modifier) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(modifier);
    synchronized (this) {
      List<ModifierEntry> newList = new ArrayList<>(entries);
      newList.removeIf(e -> e.key().equals(key));
      newList.add(new ModifierEntry(key, priority, modifier));
      newList.sort(ModifierEntry.COMPARATOR);
      this.entries = List.copyOf(newList);
    }
  }

  @Override
  public void unregister(@NonNull String key) {
    Objects.requireNonNull(key);
    synchronized (this) {
      List<ModifierEntry> newList = new ArrayList<>(entries);
      if (newList.removeIf(e -> e.key().equals(key))) {
        this.entries = List.copyOf(newList);
      }
    }
  }

  /// Applies all active modifiers' camera state transformations sequentially.
  ///
  /// Called after the base perspective establishes the target state,
  /// but before transition interpolation.
  ///
  /// Position, rotation, and FOV are validated after each modifier;
  /// invalid fields are individually reverted.
  public void applyCameraState(
      PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext ctx) {
    PerspectiveStateImpl backup = new PerspectiveStateImpl();
    for (ModifierEntry entry : entries) {
      if (!EXTENSIONS.testOrElse(entry.key(), "isAvailable", entry.modifier()::isAvailable, false))
        continue;

      backup.set(state);
      if (!EXTENSIONS.run(entry.key(), "apply", () -> entry.modifier().apply(state, ctx))) {
        restore(state, backup);
        continue;
      }
      sanitizer.sanitize(
          "modifier." + entry.key(),
          state,
          backup,
          () -> "Modifier '" + entry.key() + "' produced invalid state. Reverting.");
    }
  }

  private static void restore(
      PerspectiveState.@NonNull Mutable target, @NonNull PerspectiveState source) {
    target.position().set(source.position());
    target.rotation().set(source.rotation());
    target.setFovDeg(source.getFovDeg());
  }
}
