package io.github.leawind.perspectiveapi.internal.logic.builtin.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class OrbitMenuModelTest {
  @Test
  void initializesSelectionAndKeepsLaterRegistrationsAsCandidates() {
    OrbitMenuModel model = new OrbitMenuModel();
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
    OrbitMenuModel model = new OrbitMenuModel();
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
    OrbitMenuModel model = new OrbitMenuModel();
    model.updateSwitchables(
        List.of(perspective("a", 0, true), perspective("b", 1, false), perspective("c", 2, true)));
    model.applyLayout(List.of("c"), Set.of());
    assertEquals("a", model.cycleForward("c").info().id());
    assertEquals("c", model.cycleForward("a").info().id());
  }

  @Test
  void cycleIncludesCandidatesButSkipsDisabledPerspectives() {
    OrbitMenuModel model = new OrbitMenuModel();
    model.updateSwitchables(
        List.of(
            perspective("wheel", 0, true),
            perspective("other", 1, true),
            perspective("disabled", 2, true)));
    model.applyLayout(List.of("wheel"), Set.of("disabled"));
    assertEquals("other", model.cycleForward("wheel").info().id());
    assertEquals("wheel", model.cycleForward("other").info().id());
  }

  @Test
  void unknownCurrentSelectionStartsAtFirstAvailablePerspective() {
    OrbitMenuModel model = new OrbitMenuModel();
    model.updateSwitchables(
        List.of(perspective("a", 0, false), perspective("b", 1, true)));

    assertEquals("b", model.cycleForward("missing").info().id());
  }

  @Test
  void actorIdentitySurvivesSwitchableUpdates() {
    PerspectiveSwitcher switcher = PerspectiveSwitcher.INSTANCE;
    Perspective perspective = perspective("stable", 0, true);
    switcher.updateSwitchables(List.of(perspective));
    PerspectiveActor actor = switcher.menu().actor("stable");
    assertNotNull(actor);

    switcher.updateSwitchables(List.of(perspective));

    assertSame(actor, switcher.menu().actor("stable"));
    assertSame(actor.body(), switcher.menu().actor("stable").body());
  }

  @Test
  void disabledPerspectiveRemainsAddressableForDirectSelection() {
    OrbitMenuModel model = new OrbitMenuModel();
    model.updateSwitchables(
        List.of(perspective("active", 0, true), perspective("hovered", 1, true)));
    model.applyLayout(List.of("active"), Set.of("hovered"));

    assertNotNull(model.perspective("hovered"));
    assertEquals(OrbitMenuModel.Group.DISABLED, model.groupOf("hovered"));
  }

  @Test
  void switcherExtractsAndAppliesPersistedLayout() {
    PerspectiveSwitcher switcher = PerspectiveSwitcher.INSTANCE;
    PerspectiveSwitcherState previousState = switcher.extractState();
    switcher.updateSwitchables(
        List.of(perspective("a", 0, true), perspective("b", 1, true), perspective("c", 2, true)));

    try {
      switcher.applyState(new PerspectiveSwitcherState(List.of("c", "b"), Set.of("a"), 12, true, false));

      assertEquals(List.of("c", "b"), switcher.model().selected());
      assertEquals(Set.of("a"), switcher.model().disabled());
      assertEquals(12, switcher.getHoldTicks());
      assertEquals(
          new PerspectiveSwitcherState(List.of("c", "b"), Set.of("a"), 12, true, false),
          switcher.extractState());
    } finally {
      switcher.applyState(previousState);
    }
  }

  @Test
  void holdTicksMustStayWithinConfigRange() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PerspectiveSwitcherState(List.of(), Set.of(), -1, false, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PerspectiveSwitcherState(List.of(), Set.of(), 21, false, false));
  }

  @Test
  void oldStateDefaultsTutorialHintsToIncomplete() {
    PerspectiveSwitcherState state =
        PerspectiveSwitcherState.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).result().orElseThrow();

    assertEquals(
        new PerspectiveSwitcherState(
            List.of(), Set.of(), PerspectiveSwitcher.DEFAULT_HOLD_TICKS, false, false),
        state);
  }

  private static Perspective perspective(String id, int priority, boolean available) {
    return new TestPerspective(id, priority, available);
  }

  private record TestPerspective(@NonNull PerspectiveInfo info, boolean available)
      implements Perspective {
    private TestPerspective(@NonNull String id, int priority, boolean available) {
      this(
          PerspectiveInfo.builder(id, Component.literal(id))
              .baseType(BaseType.FIRST_PERSON)
              .priority(priority)
              .build(),
          available);
    }

    @Override
    public boolean isAvailable() {
      return available;
    }
  }
}
