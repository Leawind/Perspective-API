package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class PerspectiveOverrideChainImpl implements PerspectiveOverrideChain {
  public record Entry(
      @NonNull Identifier key, int priority, @NonNull Supplier<@Nullable Identifier> supplier) {}

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final List<Entry> entries = new ArrayList<>();

  PerspectiveOverrideChainImpl() {}

  @Override
  public void push(
      @NonNull Identifier key, int priority, @NonNull Supplier<@Nullable Identifier> supplier) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(supplier);
    try (var ignored = LockUtils.writeLock(lock)) {
      for (int i = entries.size() - 1; i >= 0; i--) {
        if (entries.get(i).key().equals(key)) {
          entries.remove(i);
        }
      }

      var entry = new Entry(key, priority, supplier);
      int insertIdx = 0;
      for (int i = 0; i < entries.size(); i++) {
        if (entries.get(i).priority() >= priority) {
          insertIdx = i + 1;
        } else {
          break;
        }
      }
      entries.add(insertIdx, entry);
    }
  }

  @Override
  public void pop(@NonNull Identifier key) {
    Objects.requireNonNull(key);
    try (var ignored = LockUtils.writeLock(lock)) {
      entries.removeIf(e -> e.key().equals(key));
    }
  }

  @Override
  public boolean has(@NonNull Identifier key) {
    Objects.requireNonNull(key);
    try (var ignored = LockUtils.readLock(lock)) {
      return entries.stream().anyMatch(e -> e.key().equals(key));
    }
  }

  @Override
  public void clear() {
    try (var ignored = LockUtils.writeLock(lock)) {
      entries.clear();
    }
  }

  @Override
  public void clearExcept(@NonNull Identifier... keys) {
    Objects.requireNonNull(keys);
    Set<Identifier> keep = new HashSet<>(Arrays.asList(keys));
    try (var ignored = LockUtils.writeLock(lock)) {
      entries.removeIf(e -> !keep.contains(e.key()));
    }
  }

  @Override
  public @Nullable Identifier resolve(@NonNull Predicate<@NonNull Identifier> validator) {
    try (var ignored = LockUtils.readLock(lock)) {
      for (Entry entry : entries) {
        Identifier id = entry.supplier().get();
        if (id != null && validator.test(id)) {
          return id;
        }
      }
    }
    return null;
  }
}
