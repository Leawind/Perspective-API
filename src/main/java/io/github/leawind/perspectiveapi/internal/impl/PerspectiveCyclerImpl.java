package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PerspectiveCyclerImpl implements PerspectiveCycler {
  private record Entry(Identifier id, int priority) {}

  private final ReadWriteLock lock;

  private final List<Entry> entries = new ArrayList<>();

  public PerspectiveCyclerImpl(ReadWriteLock lock) {
    this.lock = lock;
  }

  @Override
  public void set(@NonNull List<Identifier> list) {
    try (var ignored = LockUtils.writeLock(lock)) {
      entries.clear();
      for (int i = 0; i < list.size(); i++) {
        entries.add(new Entry(list.get(i), i));
      }
    }
  }

  @Override
  public @NonNull PerspectiveCycler add(@NonNull Identifier id, int priority) {
    Objects.requireNonNull(id);

    try (var ignored = LockUtils.writeLock(lock)) {
      entries.removeIf(e -> e.id().equals(id));
      entries.add(new Entry(id, priority));
      entries.sort(Comparator.comparingInt(Entry::priority));
    }
    return this;
  }

  @Override
  public void remove(@Nullable Identifier id) {
    try (var ignored = LockUtils.writeLock(lock)) {
      if (id == null) return;

      entries.removeIf(e -> e.id().equals(id));
    }
  }

  @Override
  public void clear() {
    try (var ignored = LockUtils.writeLock(lock)) {
      entries.clear();
    }
  }

  @Override
  public @NonNull Stream<Identifier> stream() {
    try (var ignored = LockUtils.readLock(lock)) {
      return entries.stream().map(Entry::id).toList().stream();
    }
  }

  @Override
  public @Nullable Identifier getNext(@Nullable Identifier current) {
    try (var ignored = LockUtils.readLock(lock)) {
      if (entries.isEmpty()) return null;

      int idx = indexOf(current);
      if (idx < 0) {
        return entries.get(0).id();
      }

      return entries.get((idx + 1) % entries.size()).id();
    }
  }

  @Override
  public @Nullable Identifier getPrevious(@Nullable Identifier current) {
    try (var ignored = LockUtils.readLock(lock)) {
      if (entries.isEmpty()) return null;

      int idx = indexOf(current);
      if (idx < 0) {
        return entries.get(entries.size() - 1).id();
      }

      return entries.get((idx - 1 + entries.size()) % entries.size()).id();
    }
  }

  @Override
  public @Nullable Identifier getFirst() {
    try (var ignored = LockUtils.readLock(lock)) {
      return entries.isEmpty() ? null : entries.get(0).id();
    }
  }

  private @Nullable Identifier getLast() {
    return entries.isEmpty() ? null : entries.get(entries.size() - 1).id();
  }

  private int indexOf(@Nullable Identifier id) {
    if (id == null) {
      return -1;
    }

    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).id().equals(id)) {
        return i;
      }
    }
    return -1;
  }
}
