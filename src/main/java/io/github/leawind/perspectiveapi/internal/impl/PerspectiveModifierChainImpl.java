package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveModifier;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

public final class PerspectiveModifierChainImpl implements PerspectiveModifierChain {

  /// Represents a registered modifier with its priority and key.
  private record ModifierEntry(
      @NonNull Identifier key, int priority, @NonNull PerspectiveModifier modifier) {
    static final Comparator<ModifierEntry> COMPARATOR =
        Comparator.comparingInt(ModifierEntry::priority);
  }

  private final BiConsumer<@NonNull PerspectiveModifier, @NonNull Throwable> onException;
  private final BiConsumer<@NonNull PerspectiveModifier, @NonNull String> onInvalidResult;

  private volatile List<ModifierEntry> entries = List.of();

  public PerspectiveModifierChainImpl(
      @NonNull BiConsumer<@NonNull PerspectiveModifier, @NonNull Throwable> onException,
      @NonNull BiConsumer<@NonNull PerspectiveModifier, @NonNull String> onInvalidResult) {
    this.onException = Objects.requireNonNull(onException);
    this.onInvalidResult = Objects.requireNonNull(onInvalidResult);
  }

  @Override
  public void register(
      @NonNull Identifier key, int priority, @NonNull PerspectiveModifier modifier) {
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
  public void unregister(@NonNull Identifier key) {
    Objects.requireNonNull(key);
    synchronized (this) {
      List<ModifierEntry> newList = new ArrayList<>(entries);
      if (newList.removeIf(e -> e.key().equals(key))) {
        this.entries = List.copyOf(newList);
      }
    }
  }

  @Override
  public void applyTransform(
      @NonNull PerspectiveContext ctx, @NonNull Vector3d position, @NonNull Quaternionf rotation) {
    for (ModifierEntry entry : entries) {
      if (entry.modifier().isAvailable()) {
        try {
          entry.modifier().applyTransform(ctx, position, rotation);
        } catch (Throwable e) {
          onException.accept(entry.modifier(), e);
        }
      }
    }
  }

  @Override
  public float applyFov(@NonNull PerspectiveContext ctx, float fov) {
    for (ModifierEntry entry : entries) {
      if (entry.modifier().isAvailable()) {
        try {
          float newFov = entry.modifier().applyFov(ctx, fov);
          if (Sanitizer.isFinite(newFov) && newFov >= 0.0f && newFov <= 180.0f) {
            fov = newFov;
          } else {
            onInvalidResult.accept(
                entry.modifier(), "Modifier '" + entry.key() + "' returned invalid FOV. Ignoring.");
          }
        } catch (Throwable e) {
          onException.accept(entry.modifier(), e);
        }
      }
    }
    return fov;
  }
}
