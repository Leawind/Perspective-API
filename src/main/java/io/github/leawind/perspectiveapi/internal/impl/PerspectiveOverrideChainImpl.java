package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.utils.Exceptions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerspectiveOverrideChainImpl
    implements PerspectiveOverrideChain, Supplier<@Nullable String> {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveOverrideChainImpl.class);

  public record Entry(
      @NonNull String key, int priority, @NonNull Supplier<@Nullable String> supplier) {
    public static final Comparator<Entry> COMPARATOR =
        Comparator.comparingInt(Entry::priority).reversed();
  }

  private volatile List<Entry> entries = List.of();
  private final PerspectiveRegistry registry;
  private final BiConsumer<@NonNull String, @NonNull Throwable> onException;

  public PerspectiveOverrideChainImpl(@NonNull PerspectiveRegistry registry) {
    this(
        registry,
        (key, throwable) ->
            LOGGER.warn("Override entry '{}' threw while being resolved", key, throwable));
  }

  public PerspectiveOverrideChainImpl(
      @NonNull PerspectiveRegistry registry,
      @NonNull BiConsumer<@NonNull String, @NonNull Throwable> onException) {
    this.registry = Objects.requireNonNull(registry);
    this.onException = Objects.requireNonNull(onException);
  }

  @Override
  public @Nullable String get() {
    List<Entry> snapshot = this.entries;
    for (Entry entry : snapshot) {
      try {
        String id = entry.supplier.get();
        if (id == null) continue;
        Perspective perspective = registry.get(id);
        if (perspective != null && perspective.isAvailable()) return id;
      } catch (Throwable throwable) {
        Exceptions.rethrowIfFatal(throwable);
        onException.accept(entry.key(), throwable);
      }
    }
    return null;
  }

  @Override
  public void push(
      @NonNull String key, int priority, @NonNull Supplier<@Nullable String> supplier) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(supplier);
    synchronized (this) {
      List<Entry> newList = new ArrayList<>(entries);
      newList.removeIf(e -> e.key().equals(key));
      newList.add(new Entry(key, priority, supplier));
      newList.sort(Entry.COMPARATOR);
      this.entries = List.copyOf(newList);
    }
  }

  @Override
  public void pop(@NonNull String key) {
    Objects.requireNonNull(key);
    synchronized (this) {
      List<Entry> newList = new ArrayList<>(entries);
      if (newList.removeIf(e -> e.key().equals(key))) {
        this.entries = List.copyOf(newList);
      }
    }
  }

  @Override
  public boolean has(@NonNull String key) {
    Objects.requireNonNull(key);
    for (Entry entry : entries) {
      if (entry.key().equals(key)) return true;
    }
    return false;
  }

  /// Clears all entries from the chain.
  public void clear() {
    synchronized (this) {
      if (!entries.isEmpty()) {
        this.entries = List.of();
      }
    }
  }

  /// Clears all entries except those with the specified keys.
  public void clearExcept(@NonNull String... keys) {
    Objects.requireNonNull(keys);
    Set<String> keep = new HashSet<>(Arrays.asList(keys));
    synchronized (this) {
      List<Entry> newList = new ArrayList<>(entries);
      if (newList.removeIf(e -> !keep.contains(e.key()))) {
        this.entries = List.copyOf(newList);
      }
    }
  }
}
