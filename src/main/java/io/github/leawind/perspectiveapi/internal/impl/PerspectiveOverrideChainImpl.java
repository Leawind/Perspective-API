package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class PerspectiveOverrideChainImpl
    implements PerspectiveOverrideChain, Supplier<Identifier> {
  public record Entry(
      @NonNull Identifier key, int priority, @NonNull Supplier<Identifier> supplier) {
    public static Comparator<Entry> COMPARATOR = Comparator.comparingInt(e -> -e.priority);
  }

  private volatile List<Entry> entries = List.of();
  private final PerspectiveRegistry registry;

  public PerspectiveOverrideChainImpl(PerspectiveRegistry registry) {
    this.registry = registry;
  }

  @Override
  public @Nullable Identifier get() {
    List<Entry> snapshot = this.entries;
    for (Entry entry : snapshot) {
      Identifier id = entry.supplier.get();
      if (id != null && registry.contains(id)) {
        return id;
      }
    }
    return null;
  }

  @Override
  public void push(@NonNull Identifier key, int priority, @NonNull Supplier<Identifier> supplier) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(supplier);
    synchronized (this) {
      List<Entry> newList = new ArrayList<>(entries);
      newList.removeIf(e -> e.key().equals(key));
      newList.add(new Entry(key, priority, supplier));
      newList.sort(Entry.COMPARATOR);
      this.entries = newList;
    }
  }

  @Override
  public void pop(@NonNull Identifier key) {
    Objects.requireNonNull(key);
    synchronized (this) {
      List<Entry> newList = new ArrayList<>(entries);
      if (newList.removeIf(e -> e.key().equals(key))) {
        this.entries = List.copyOf(newList);
      }
    }
  }

  @Override
  public boolean has(@NonNull Identifier key) {
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
  public void clearExcept(@NonNull Identifier... keys) {
    Objects.requireNonNull(keys);
    Set<Identifier> keep = new HashSet<>(Arrays.asList(keys));
    synchronized (this) {
      List<Entry> newList = new ArrayList<>(entries);
      if (newList.removeIf(e -> !keep.contains(e.key()))) {
        this.entries = List.copyOf(newList);
      }
    }
  }
}
