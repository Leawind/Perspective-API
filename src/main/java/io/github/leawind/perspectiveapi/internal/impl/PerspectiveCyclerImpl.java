package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class PerspectiveCyclerImpl implements PerspectiveCycler {
  public static final Identifier KEY = Bridge.createIdentifier(PerspectiveAPI.MOD_ID, "cycler");

  private record Entry(Identifier id, int priority) {}

  private volatile List<Entry> entries = List.of();
  private volatile @Nullable Identifier activeId;
  private volatile boolean useCustomOrder;
  private volatile List<Identifier> customOrder = List.of();

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
    this.entries = List.of();
    this.activeId = null;
  }

  @Override
  public boolean isCustomOrderEnabled() {
    return useCustomOrder;
  }

  @Override
  public void setCustomOrderEnabled(boolean useCustomOrder) {
    this.useCustomOrder = useCustomOrder;
  }

  @Override
  public void setCustomOrder(@NonNull List<@NonNull Identifier> orderedIds) {
    Objects.requireNonNull(orderedIds);
    this.customOrder = List.copyOf(orderedIds);
  }

  @Override
  public @NonNull List<@NonNull Identifier> getIds() {
    if (!useCustomOrder) {
      return entries.stream().map(Entry::id).toList();
    }
    List<Entry> snapshot = this.entries;
    List<Identifier> idsInCycler = snapshot.stream().map(Entry::id).toList();
    List<Identifier> result = new ArrayList<>();
    for (Identifier id : customOrder) {
      if (idsInCycler.contains(id) && !result.contains(id)) {
        result.add(id);
      }
    }
    for (Identifier id : idsInCycler) {
      if (!result.contains(id)) {
        result.add(id);
      }
    }
    return List.copyOf(result);
  }

  @Override
  public @Nullable Identifier getNext(@Nullable Identifier current) {
    List<Identifier> list = getIds();
    if (list.isEmpty()) return null;
    int idx = indexOfId(list, current);
    if (idx < 0) return list.get(0);
    return list.get((idx + 1) % list.size());
  }

  @Override
  public @Nullable Identifier getPrevious(@Nullable Identifier current) {
    List<Identifier> list = getIds();
    if (list.isEmpty()) return null;
    int idx = indexOfId(list, current);
    if (idx < 0) return list.get(list.size() - 1);
    return list.get((idx - 1 + list.size()) % list.size());
  }

  @Override
  public @Nullable Identifier getActiveId() {
    return activeId;
  }

  @Override
  public void setActiveId(@Nullable Identifier id) {
    this.activeId = id;
  }

  @Override
  public void cycleForward(@NonNull PerspectiveRegistry registry) {
    List<Identifier> list = getIds();
    if (list.isEmpty()) return;

    Identifier current = activeId;
    Identifier candidate = current;
    int attempts = 0;
    int size = list.size();

    do {
      candidate = nextInList(list, candidate);
      if (candidate != null && registry.contains(candidate)) {
        activeId = candidate;
        return;
      }
      attempts++;
    } while (attempts < size && !Objects.equals(candidate, current));
  }

  @Override
  public void cycleBackward(@NonNull PerspectiveRegistry registry) {
    List<Identifier> list = getIds();
    if (list.isEmpty()) return;

    Identifier current = activeId;
    Identifier candidate = current;
    int attempts = 0;
    int size = list.size();

    do {
      candidate = previousInList(list, candidate);
      if (candidate == null) return;
      if (registry.contains(candidate)) {
        activeId = candidate;
        return;
      }
      attempts++;
    } while (attempts < size && !Objects.equals(candidate, current));
  }

  private static @Nullable Identifier nextInList(
      List<Identifier> list, @Nullable Identifier current) {
    if (list.isEmpty()) return null;
    int idx = indexOfId(list, current);
    if (idx < 0) return list.get(0);
    return list.get((idx + 1) % list.size());
  }

  private static @Nullable Identifier previousInList(
      List<Identifier> list, @Nullable Identifier current) {
    if (list.isEmpty()) return null;
    int idx = indexOfId(list, current);
    if (idx < 0) return list.get(list.size() - 1);
    return list.get((idx - 1 + list.size()) % list.size());
  }

  private static int indexOfId(List<Identifier> list, @Nullable Identifier id) {
    if (id == null) return -1;
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).equals(id)) return i;
    }
    return -1;
  }
}
