package io.github.leawind.perspectiveapi.internal.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class PerspectiveSelectionImplTest {
  @Test
  void notifiesOnlyWhenSelectedIdChanges() {
    PerspectiveSelectionImpl selection = new PerspectiveSelectionImpl();
    AtomicInteger changes = new AtomicInteger();
    List<String> selectedIds = new ArrayList<>();
    selection.onChanged(
        selectedId -> {
          changes.incrementAndGet();
          selectedIds.add(selectedId);
        });

    selection.setSelectedPerspective(perspective("test.first"));
    selection.setSelectedPerspective(perspective("test.first"));

    assertEquals("test.first", selection.selectedPerspectiveId());
    assertEquals(1, changes.get());
    assertEquals(List.of("test.first"), selectedIds);
  }

  @Test
  void restoreRetainsRawIdsAndCanClearSelection() {
    PerspectiveSelectionImpl selection = new PerspectiveSelectionImpl();
    List<String> changes = new ArrayList<>();
    selection.onChanged(changes::add);

    selection.restore("missing.perspective");
    assertEquals("missing.perspective", selection.selectedPerspectiveId());

    selection.restore(null);
    assertNull(selection.selectedPerspectiveId());
    assertEquals(2, changes.size());
    assertEquals("missing.perspective", changes.get(0));
    assertNull(changes.get(1));
  }

  @Test
  void listenerFailuresDoNotPreventLaterListeners() {
    PerspectiveSelectionImpl selection = new PerspectiveSelectionImpl();
    AtomicInteger successfulCalls = new AtomicInteger();
    selection.onChanged(
        selectedId -> {
          throw new IllegalStateException("listener failure");
        });
    selection.onChanged(selectedId -> successfulCalls.incrementAndGet());

    selection.setSelectedPerspective(perspective("test.selected"));

    assertEquals(1, successfulCalls.get());
  }

  @Test
  void reentrantChangesAreQueuedUntilCurrentNotificationCompletes() {
    PerspectiveSelectionImpl selection = new PerspectiveSelectionImpl();
    Perspective second = perspective("test.second");
    List<String> calls = new ArrayList<>();
    selection.onChanged(
        selectedId -> {
          calls.add("first:" + selectedId);
          if (selectedId.equals("test.first")) selection.setSelectedPerspective(second);
        });
    selection.onChanged(selectedId -> calls.add("second:" + selectedId));

    selection.setSelectedPerspective(perspective("test.first"));

    assertEquals(
        List.of(
            "first:test.first", "second:test.first", "first:test.second", "second:test.second"),
        calls);
    assertEquals("test.second", selection.selectedPerspectiveId());
  }

  @Test
  void registrationUnregistersOnlyItself() {
    PerspectiveSelectionImpl selection = new PerspectiveSelectionImpl();
    AtomicInteger firstCalls = new AtomicInteger();
    AtomicInteger secondCalls = new AtomicInteger();
    var first = selection.onChanged(selectedId -> firstCalls.incrementAndGet());
    selection.onChanged(selectedId -> secondCalls.incrementAndGet());

    assertTrue(first.unregister());
    assertFalse(first.unregister());
    selection.setSelectedPerspective(perspective("test.selected"));

    assertEquals(0, firstCalls.get());
    assertEquals(1, secondCalls.get());
  }

  private static @NonNull Perspective perspective(@NonNull String id) {
    return new Perspective() {
      private final PerspectiveInfo info =
          PerspectiveInfo.builder(id, Component.literal(id)).build();

      @Override
      public @NonNull PerspectiveInfo info() {
        return info;
      }

      @Override
      public boolean isAvailable() {
        return true;
      }
    };
  }
}
