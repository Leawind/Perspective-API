package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class PerspectiveCyclerImpl implements PerspectiveCycler {
  public static final Identifier KEY = Bridge.createIdentifier(PerspectiveAPI.MOD_ID, "cycler");

  private record Entry(Identifier id, int priority) {}

  private volatile List<Entry> entries = List.of();
  private volatile @Nullable Identifier activeId;

  PerspectiveCyclerImpl() {}

  @Override
  public @NonNull PerspectiveCycler add(@NonNull Identifier id, int priority) {
    Objects.requireNonNull(id);
    synchronized (this) {
      List<Entry> newList = new ArrayList<>(entries);
      newList.removeIf(e -> e.id().equals(id));
      newList.add(new Entry(id, priority));
      newList.sort(Comparator.comparingInt(Entry::priority));
      this.entries = List.copyOf(newList);
    }
    return this;
  }

  @Override
  public void remove(@Nullable Identifier id) {
    if (id == null) return;
    synchronized (this) {
      List<Entry> newList = new ArrayList<>(entries);
      if (newList.removeIf(e -> e.id().equals(id))) {
        this.entries = List.copyOf(newList);
      }
    }
  }

  @Override
  public synchronized void clear() {
    if (!entries.isEmpty()) {
      this.entries = List.of();
    }
  }

  @Override
  public @NonNull Stream<Identifier> stream() {
    return entries.stream().map(Entry::id);
  }

  @Override
  public boolean isEmpty() {
    return entries.isEmpty();
  }

  @Override
  public @Nullable Identifier getNext(@Nullable Identifier current) {
    List<Entry> snapshot = this.entries;
    if (snapshot.isEmpty()) {
      return null;
    }
    int idx = indexOf(snapshot, current);
    if (idx < 0) return snapshot.get(0).id();
    return snapshot.get((idx + 1) % snapshot.size()).id();
  }

  @Override
  public @Nullable Identifier getPrevious(@Nullable Identifier current) {
    List<Entry> snapshot = this.entries;
    if (snapshot.isEmpty()) {
      return null;
    }
    int idx = indexOf(snapshot, current);
    if (idx < 0) return snapshot.get(snapshot.size() - 1).id();
    return snapshot.get((idx - 1 + snapshot.size()) % snapshot.size()).id();
  }

  @Override
  public @Nullable Identifier getFirst() {
    List<Entry> snapshot = this.entries;
    return snapshot.isEmpty() ? null : snapshot.get(0).id();
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
    List<Entry> snapshot = this.entries;
    if (snapshot.isEmpty()) return;

    Identifier current = activeId;
    Identifier candidate = current;
    int attempts = 0;
    int size = snapshot.size();

    do {
      candidate = getNextInSnapshot(snapshot, candidate);
      if (candidate != null && registry.contains(candidate)) {
        activeId = candidate;
        return;
      }
      attempts++;
    } while (attempts < size && !Objects.equals(candidate, current));
  }

  @Override
  public void switchToPreviousAvailable(@NonNull PerspectiveRegistry registry) {
    List<Entry> snapshot = this.entries;
    if (snapshot.isEmpty()) return;

    Identifier current = activeId;
    Identifier candidate = current;
    int attempts = 0;
    int size = snapshot.size();

    do {
      candidate = getPreviousInSnapshot(snapshot, candidate);
      if (candidate == null) return;
      if (registry.contains(candidate)) {
        activeId = candidate;
        return;
      }
      attempts++;
    } while (attempts < size && !Objects.equals(candidate, current));
  }

  private static @Nullable Identifier getNextInSnapshot(
      List<Entry> snapshot, @Nullable Identifier current) {
    if (snapshot.isEmpty()) {
      return null;
    }
    int idx = indexOf(snapshot, current);
    if (idx < 0) return snapshot.get(0).id();
    return snapshot.get((idx + 1) % snapshot.size()).id();
  }

  /// @return previous entry, or `null` if list is empty
  private static @Nullable Identifier getPreviousInSnapshot(
      List<Entry> snapshot, @Nullable Identifier current) {
    if (snapshot.isEmpty()) {
      return null;
    }
    int idx = indexOf(snapshot, current);
    if (idx < 0) {
      return snapshot.get(snapshot.size() - 1).id();
    }
    return snapshot.get((idx - 1 + snapshot.size()) % snapshot.size()).id();
  }

  private static int indexOf(List<Entry> entries, @Nullable Identifier id) {
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
