package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.PerspectiveWheel;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PerspectiveWheelImpl implements PerspectiveWheel, Supplier<Identifier> {
  public static final Identifier KEY = Bridge.createIdentifier(PerspectiveAPI.MOD_ID, "wheel");

  private final PerspectiveRegistry registry;

  private volatile @Nullable Identifier activeId;

  /// Temporary override set by the wheel menu while it is open.
  /// When non-null, {@link #get()} returns this instead of the real active perspective.
  private volatile @Nullable Identifier previewId;
  private final List<Identifier> selected = new ArrayList<>();
  private final Set<Identifier> disabled = new HashSet<>();

  private final Map<Identifier, Integer> priorities = new HashMap<>();
  private final TreeSet<Identifier> candidate;

  public PerspectiveWheelImpl(PerspectiveRegistry registry) {
    this.registry = registry;
    candidate =
        new TreeSet<>(
            Comparator.comparingInt((Identifier id) -> priorities.getOrDefault(id, 0))
                .thenComparing(Identifier::toString));
  }

  @Override
  public @Nullable Identifier get() {
    var previewId = this.previewId;
    if (previewId != null) return previewId;

    var list = getOrdered();
    var activeId = this.activeId;
    int start = list.indexOf(activeId);
    if (start < 0) start = 0;

    int size = list.size();
    for (int i = 0; i < size; i++) {
      Identifier id = list.get((start + i) % size);
      var p = registry.get(id);
      if (p != null && p.isAvailable()) return id;
    }
    return null;
  }

  @Override
  public synchronized @NonNull PerspectiveWheelImpl register(@NonNull Identifier id, int priority) {
    Objects.requireNonNull(id);
    priorities.put(id, priority);

    if (selected.contains(id)) return this;
    if (disabled.contains(id)) return this;

    candidate.add(id);

    return this;
  }

  @Override
  public synchronized void unregister(@NonNull Identifier id) {
    priorities.remove(id);
    candidate.remove(id);
  }

  public synchronized void disable(@NonNull Identifier id) {
    selected.remove(id);
    candidate.remove(id);

    disabled.add(id);
  }

  public synchronized void selectAt(int insertIndex, @NonNull Identifier... ids) {
    for (int i = ids.length - 1; i >= 0; i--) {
      Identifier id = ids[i];

      disabled.remove(id);
      candidate.remove(id);

      int oldIndex = selected.indexOf(id);
      if (oldIndex >= 0) {
        selected.remove(oldIndex);
        if (oldIndex < insertIndex) insertIndex--;
      }
      selected.add(insertIndex, id);
    }
  }

  public synchronized void deselect(@NonNull Identifier @NonNull ... ids) {
    for (Identifier id : ids) {
      disabled.remove(id);
      selected.remove(id);

      candidate.add(id);
    }
  }

  public synchronized void setSelected(@NonNull List<@NonNull Identifier> ids) {
    Set<Identifier> newSelected = new HashSet<>(ids);

    // Move old selected items not in new list back to candidate
    for (Identifier id : selected) {
      if (!newSelected.contains(id)) {
        candidate.add(id);
      }
    }

    selected.clear();
    for (Identifier id : ids) {
      if (registry.contains(id) && !selected.contains(id)) {
        selected.add(id);
        candidate.remove(id);
      }
    }
  }

  public List<@NonNull Identifier> getSelected() {
    return selected;
  }

  /// Sets the active perspective directly, bypassing the cycling logic.
  public synchronized void setActive(@NonNull Identifier id) {
    Objects.requireNonNull(id);
    this.activeId = id;
  }

  /// Sets a temporary preview override. While non-null, {@link #get()} returns this ID.
  public void setPreview(@NonNull Identifier id) {
    Objects.requireNonNull(id);
    this.previewId = id;
  }

  /// Clears the preview override, restoring normal {@link #get()} behavior.
  public void clearPreview() {
    this.previewId = null;
  }

  TreeSet<@NonNull Identifier> getCandidates() {
    return candidate;
  }

  public Set<@NonNull Identifier> getDisabled() {
    return disabled;
  }

  public List<Identifier> getOrdered() {
    List<Identifier> list = new ArrayList<>(selected.size() + candidate.size());
    for (Identifier id : selected) {
      if (priorities.containsKey(id)) list.add(id);
    }
    list.addAll(candidate);
    return list;
  }

  /// Advances the active perspective to the next available one.
  public synchronized void cycleForward() {
    var list = getOrdered();
    if (list.isEmpty()) return;

    Identifier current = get();
    int idx = list.indexOf(current);
    int start = idx < 0 ? 0 : (idx + 1) % list.size();

    int attempts = 0;
    int size = list.size();
    int i = start;
    do {
      Identifier next = list.get(i);
      var p = registry.get(next);
      if (p != null && p.isAvailable()) {
        activeId = next;
        return;
      }
      i = (i + 1) % size;
      attempts++;
    } while (attempts < size);
  }

  /// Moves the active perspective to the previous available one.
  public synchronized void cycleBackward() {
    var list = getOrdered();
    if (list.isEmpty()) return;

    Identifier current = get();
    int idx = list.indexOf(current);
    int size = list.size();
    int i = idx < 0 ? size - 1 : (idx - 1 + size) % size;

    int attempts = 0;
    do {
      Identifier next = list.get(i);
      var p = registry.get(next);
      if (p != null && p.isAvailable()) {
        activeId = next;
        return;
      }
      i = (i - 1 + size) % size;
      attempts++;
    } while (attempts < size);
  }

  public synchronized void restore(
      @Nullable Identifier activeId, List<Identifier> selected, Set<Identifier> disabled) {
    this.activeId = activeId;

    this.selected.clear();
    this.selected.addAll(selected);

    this.disabled.clear();
    this.disabled.addAll(disabled);
  }
}
