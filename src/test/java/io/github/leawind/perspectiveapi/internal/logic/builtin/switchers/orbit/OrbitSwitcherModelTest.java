package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class OrbitSwitcherModelTest {
  @Test
  void initializesSelectionAndKeepsLaterRegistrationsAsCandidates() {
    OrbitSwitcherModel model = new OrbitSwitcherModel();
    Perspective second = perspective("second", 2, true);
    Perspective first = perspective("first", 1, true);
    model.updateSwitchables(List.of(second, first));

    assertEquals(List.of("first", "second"), model.selected());

    Perspective added = perspective("added", 0, true);
    model.updateSwitchables(List.of(second, added, first));

    assertEquals(List.of("first", "second"), model.selected());
    assertEquals(List.of("added"), model.candidates());
  }

  @Test
  void layoutAlwaysPartitionsEverySwitchable() {
    OrbitSwitcherModel model = new OrbitSwitcherModel();
    model.updateSwitchables(
        List.of(perspective("a", 0, true), perspective("b", 1, true), perspective("c", 2, true)));

    model.applyLayout(List.of("c", "c"), Set.of("a", "c", "missing"));

    assertEquals(List.of("c"), model.selected());
    assertEquals(List.of("b"), model.candidates());
    assertEquals(Set.of("a"), model.disabled());
    Set<String> union = new HashSet<>(model.selected());
    union.addAll(model.candidates());
    union.addAll(model.disabled());
    assertEquals(Set.of("a", "b", "c"), union);
  }

  @Test
  void cycleUsesSelectedThenPriorityOrderedCandidatesAndSkipsUnavailable() {
    OrbitSwitcherModel model = new OrbitSwitcherModel();
    model.updateSwitchables(
        List.of(perspective("a", 0, true), perspective("b", 1, false), perspective("c", 2, true)));
    model.applyLayout(List.of("c"), Set.of());
    model.activate("c");

    model.cycleForward();
    assertEquals("a", model.resolvedId());
    model.cycleForward();
    assertEquals("c", model.resolvedId());
  }

  @Test
  void noAvailablePerspectiveProducesNoSelection() {
    OrbitSwitcherModel model = new OrbitSwitcherModel();
    model.updateSwitchables(List.of(perspective("a", 0, false)));

    model.ensureActive();

    assertNull(model.resolvedId());
  }

  @Test
  void unavailablePerspectiveCannotBePreviewed() {
    OrbitSwitcherModel model = new OrbitSwitcherModel();
    model.updateSwitchables(
        List.of(perspective("available", 0, true), perspective("unavailable", 1, false)));
    model.activate("available");

    model.preview("unavailable");

    assertEquals("available", model.resolvedId());
    assertFalse(model.disabled().contains("unavailable"));
  }

  @Test
  void actorIdentitySurvivesSwitchableUpdates() {
    OrbitSwitcherBehavior switcher = new OrbitSwitcherBehavior();
    Perspective perspective = perspective("stable", 0, true);
    switcher.onSwitchablePerspectivesUpdated(List.of(perspective));
    PerspectiveActor actor = switcher.menu().actor("stable");
    assertNotNull(actor);

    switcher.onSwitchablePerspectivesUpdated(List.of(perspective));

    assertSame(actor, switcher.menu().actor("stable"));
    assertSame(actor.body(), switcher.menu().actor("stable").body());
  }

  @Test
  void disablingHoveredPerspectiveClearsPreview() {
    OrbitSwitcherModel model = new OrbitSwitcherModel();
    model.updateSwitchables(
        List.of(perspective("active", 0, true), perspective("hovered", 1, true)));
    model.activate("active");
    model.preview("hovered");

    model.applyLayout(List.of("active"), Set.of("hovered"));

    assertEquals("active", model.resolvedId());
  }

  private static Perspective perspective(String id, int priority, boolean available) {
    return new TestPerspective(id, priority, available);
  }

  private record TestPerspective(@NonNull String id, int priority, boolean available)
      implements Perspective {
    @Override
    public @NonNull Component name() {
      return Component.literal(id);
    }

    @Override
    public @Nullable Component description() {
      return null;
    }

    @Override
    public @NonNull BaseType baseType() {
      return BaseType.FIRST_PERSON;
    }

    @Override
    public boolean switchable() {
      return true;
    }

    @Override
    public @Nullable Identifier icon() {
      return null;
    }

    @Override
    public boolean isAvailable() {
      return available;
    }
  }
}
