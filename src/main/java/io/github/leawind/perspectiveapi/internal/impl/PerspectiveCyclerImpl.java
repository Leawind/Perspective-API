package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PerspectiveCyclerImpl implements PerspectiveCycler {
  public static final Identifier KEY = Bridge.createIdentifier(PerspectiveAPI.MOD_ID, "cycler");

  private record Entry(Identifier id, int priority) {}

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final List<Entry> entries = new ArrayList<>();

  private volatile @Nullable Identifier activeId;

  public PerspectiveCyclerImpl() {}

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
    if (id == null) return;
    try (var ignored = LockUtils.writeLock(lock)) {
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
  public boolean isEmpty() {
    return entries.isEmpty();
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

  @Override
  public @Nullable Identifier getActive() {
    return activeId;
  }

  @Override
  public void setActive(@Nullable Identifier id) {
    this.activeId = id;
  }

  @Override
  public void switchToNextAvailable(@NonNull PerspectiveRegistry registry) {
    try (var ignored = LockUtils.readLock(lock)) {
      if (entries.isEmpty()) return;

      Identifier current = activeId;
      Identifier next = current;
      do {
        next = getNext(next);
        if (!registry.contains(next)) continue;
        activeId = next;
        return;
      } while (next != current);
    }
  }

  @Override
  public void switchToPreviousAvailable(@NonNull PerspectiveRegistry registry) {
    try (var ignored = LockUtils.readLock(lock)) {
      if (entries.isEmpty()) return;

      Identifier current = activeId;
      Identifier previous = current;
      do {
        previous = getPrevious(previous);
        if (!registry.contains(previous)) continue;
        activeId = previous;
        return;
      } while (previous != current);
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
