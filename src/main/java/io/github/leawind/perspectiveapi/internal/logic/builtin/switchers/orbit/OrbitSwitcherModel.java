package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

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

final class OrbitSwitcherModel {
  enum Group {
    SELECTED,
    CANDIDATE,
    DISABLED
  }

  private static final Comparator<Perspective> PRIORITY_ORDER =
      Comparator.comparingInt(Perspective::priority).thenComparing(Perspective::id);

  private final Map<String, Perspective> switchables = new LinkedHashMap<>();
  private final List<String> selected = new ArrayList<>();
  private final Set<String> disabled = new HashSet<>();
  private boolean initialized;
  private @Nullable String activeId;
  private @Nullable String previewId;

  void updateSwitchables(@NonNull List<@NonNull Perspective> perspectives) {
    Objects.requireNonNull(perspectives);
    Set<String> incomingIds = new HashSet<>();
    for (Perspective perspective : perspectives) {
      Objects.requireNonNull(perspective);
      if (!perspective.switchable()) continue;
      incomingIds.add(perspective.id());
      switchables.put(perspective.id(), perspective);
    }

    switchables.keySet().removeIf(id -> !incomingIds.contains(id));
    selected.removeIf(id -> !incomingIds.contains(id));
    disabled.removeIf(id -> !incomingIds.contains(id));
    if (activeId != null && !incomingIds.contains(activeId)) activeId = null;
    if (previewId != null && !incomingIds.contains(previewId)) previewId = null;

    if (!initialized && !switchables.isEmpty()) {
      initialized = true;
      selected.clear();
      switchables.values().stream()
          .sorted(PRIORITY_ORDER)
          .map(Perspective::id)
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
        .filter(perspective -> groupOf(perspective.id()) == Group.CANDIDATE)
        .sorted(PRIORITY_ORDER)
        .map(Perspective::id)
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
    if (previewId != null && !isEnabled(previewId)) previewId = null;
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

  void activate(@Nullable String id) {
    if (id == null || !isEnabled(id)) return;
    activeId = id;
  }

  void preview(@Nullable String id) {
    previewId = id != null && isEnabled(id) && isAvailable(id) ? id : null;
  }

  void clearPreview() {
    previewId = null;
  }

  @Nullable String resolvedId() {
    return previewId != null ? previewId : activeId;
  }

  void ensureActive() {
    if (activeId != null && isEnabled(activeId) && isAvailable(activeId)) return;
    activeId = firstAvailable();
  }

  void cycleForward() {
    cycle(1);
  }

  void cycleBackward() {
    cycle(-1);
  }

  private void cycle(int direction) {
    List<String> ordered = orderedEnabled();
    if (ordered.isEmpty()) {
      activeId = null;
      return;
    }

    int current = ordered.indexOf(activeId);
    if (current < 0) current = direction > 0 ? -1 : 0;
    int size = ordered.size();
    for (int offset = 1; offset <= size; offset++) {
      int index = Math.floorMod(current + direction * offset, size);
      String candidate = ordered.get(index);
      if (isAvailable(candidate)) {
        activeId = candidate;
        return;
      }
    }
    activeId = null;
  }

  private @Nullable String firstAvailable() {
    return orderedEnabled().stream().filter(this::isAvailable).findFirst().orElse(null);
  }

  private @NonNull List<@NonNull String> orderedEnabled() {
    List<String> result = new ArrayList<>(selected);
    result.addAll(candidates());
    return result;
  }

  private boolean isEnabled(String id) {
    return switchables.containsKey(id) && !disabled.contains(id);
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
