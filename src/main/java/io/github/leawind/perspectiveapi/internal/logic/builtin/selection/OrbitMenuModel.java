package io.github.leawind.perspectiveapi.internal.logic.builtin.selection;

import io.github.leawind.perspectiveapi.api.Perspective;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class OrbitMenuModel {
  enum Group {
    SELECTED,
    CANDIDATE,
    DISABLED
  }

  private static final Comparator<Perspective> PERSPECTIVE_ORDER =
      Comparator.comparingInt((Perspective perspective) -> perspective.info().order())
          .thenComparing(perspective -> perspective.info().id());

  private final Map<String, Perspective> switchables = new LinkedHashMap<>();
  private final List<String> selected = new ArrayList<>();
  private final Set<String> disabled = new HashSet<>();
  private boolean initialized;

  void updateSwitchables(@NonNull List<@NonNull Perspective> perspectives) {
    Objects.requireNonNull(perspectives);
    Set<String> incomingIds = new HashSet<>();
    for (Perspective perspective : perspectives) {
      Objects.requireNonNull(perspective);
      if (!perspective.info().hasTrait("switchable")) continue;
      incomingIds.add(perspective.info().id());
      switchables.put(perspective.info().id(), perspective);
    }

    switchables.keySet().removeIf(id -> !incomingIds.contains(id));
    selected.removeIf(id -> !incomingIds.contains(id));
    disabled.removeIf(id -> !incomingIds.contains(id));
    if (!initialized && !switchables.isEmpty()) {
      initialized = true;
      selected.clear();
      switchables.values().stream()
          .sorted(PERSPECTIVE_ORDER)
          .map(perspective -> perspective.info().id())
          .forEach(selected::add);
    }

    checkInvariants();
  }

  @NonNull Collection<@NonNull Perspective> switchables() {
    return List.copyOf(switchables.values());
  }

  @NonNull List<@NonNull String> selected() {
    return List.copyOf(selected);
  }

  @NonNull List<@NonNull String> candidates() {
    return switchables.values().stream()
        .filter(perspective -> groupOf(perspective.info().id()) == Group.CANDIDATE)
        .sorted(PERSPECTIVE_ORDER)
        .map(perspective -> perspective.info().id())
        .toList();
  }

  @NonNull Set<@NonNull String> disabled() {
    return Set.copyOf(disabled);
  }

  @NonNull Group groupOf(@NonNull String id) {
    Objects.requireNonNull(id);
    if (selected.contains(id)) return Group.SELECTED;
    if (disabled.contains(id)) return Group.DISABLED;
    if (switchables.containsKey(id)) return Group.CANDIDATE;
    throw new IllegalArgumentException("Unknown switchable perspective: " + id);
  }

  @Nullable Perspective perspective(@NonNull String id) {
    return switchables.get(Objects.requireNonNull(id));
  }

  void applyLayout(
      @NonNull List<@NonNull String> selectedIds, @NonNull Set<@NonNull String> disabledIds) {
    Objects.requireNonNull(selectedIds);
    Objects.requireNonNull(disabledIds);

    List<String> newSelected = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (String id : selectedIds) {
      Objects.requireNonNull(id);
      if (switchables.containsKey(id) && seen.add(id)) newSelected.add(id);
    }

    Set<String> newDisabled = new HashSet<>();
    for (String id : disabledIds) {
      Objects.requireNonNull(id);
      if (switchables.containsKey(id) && !seen.contains(id)) newDisabled.add(id);
    }

    selected.clear();
    selected.addAll(newSelected);
    disabled.clear();
    disabled.addAll(newDisabled);
    checkInvariants();
  }

  void rotateSelected(boolean clockwise) {
    if (selected.size() < 2) return;
    if (clockwise) {
      selected.add(0, selected.remove(selected.size() - 1));
    } else {
      selected.add(selected.remove(0));
    }
  }

  @Nullable Perspective cycleForward(@Nullable String currentId) {
    return cycle(currentId, 1);
  }

  private @Nullable Perspective cycle(@Nullable String currentId, int direction) {
    List<String> ordered = orderedEnabled();
    if (ordered.isEmpty()) return null;

    int current = ordered.indexOf(currentId);
    if (current < 0) current = direction > 0 ? -1 : 0;
    int size = ordered.size();
    for (int offset = 1; offset <= size; offset++) {
      int index = Math.floorMod(current + direction * offset, size);
      String candidate = ordered.get(index);
      if (isAvailable(candidate)) return switchables.get(candidate);
    }
    return null;
  }

  private @NonNull List<@NonNull String> orderedEnabled() {
    List<String> result = new ArrayList<>(selected);
    result.addAll(candidates());
    return result;
  }

  private boolean isAvailable(String id) {
    Perspective perspective = switchables.get(id);
    return perspective != null && perspective.isAvailable();
  }

  private void checkInvariants() {
    if (new HashSet<>(selected).size() != selected.size()) {
      throw new IllegalStateException("selected contains duplicates");
    }
    if (!switchables.keySet().containsAll(selected)
        || !switchables.keySet().containsAll(disabled)) {
      throw new IllegalStateException("groups contain unregistered perspectives");
    }
    for (String id : selected) {
      if (disabled.contains(id))
        throw new IllegalStateException("perspective belongs to two groups");
    }
  }
}
