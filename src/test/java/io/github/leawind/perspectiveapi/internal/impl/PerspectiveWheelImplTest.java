package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerspectiveWheelImplTest {

  private PerspectiveRegistryImpl registry;
  private PerspectiveWheelImpl wheel;

  private static Identifier id(String path) {
    return Bridge.createIdentifier("test", path);
  }

  private static Perspective perspective(String path) {
    Identifier perspectiveId = id(path);
    return new Perspective() {
      @Override
      public @NonNull Identifier id() {
        return perspectiveId;
      }

      @Override
      public @NonNull CameraType cameraType() {
        return CameraType.FIRST_PERSON;
      }
    };
  }

  private record MutablePerspective(Identifier id, boolean[] available) implements Perspective {
    @Override
    public @NonNull CameraType cameraType() {
      return CameraType.FIRST_PERSON;
    }

    @Override
    public boolean isAvailable() {
      return available[0];
    }
  }

  private static MutablePerspective mutablePerspective(String path, boolean available) {
    return new MutablePerspective(id(path), new boolean[] {available});
  }

  private void registerMutable(int priority, MutablePerspective perspective) {
    registry.register(perspective);
    wheel.register(perspective.id(), priority);
  }

  private void registerBoth(int priority, Identifier... ids) {
    for (Identifier id : ids) {
      registry.register(perspective(id.getPath()));
      wheel.register(id, priority);
    }
  }

  @BeforeEach
  void beforeEach() {
    registry = new PerspectiveRegistryImpl(perspective("default"));
    wheel = new PerspectiveWheelImpl(registry);
  }

  // ========== register / unregister ==========

  @Test
  void registerAddsToCandidate() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);

    assertTrue(wheel.getCandidates().contains(a));
  }

  @Test
  void registerDoesNotDuplicateIntoSelected() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);
    wheel.register(a, 0);

    assertEquals(1, wheel.getOrdered().size());
  }

  @Test
  void registerExistingSelectedDoesNothing() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);
    wheel.selectAt(0, a);
    wheel.register(a, 1);

    assertTrue(wheel.getSelected().contains(a));
    assertFalse(wheel.getCandidates().contains(a));
  }

  @Test
  void registerExistingDisabledDoesNothing() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);
    wheel.disable(a);
    wheel.register(a, 2);

    assertTrue(wheel.getDisabled().contains(a));
    assertFalse(wheel.getCandidates().contains(a));
  }

  @Test
  void unregisterRemovesFromCandidate() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);
    wheel.unregister(a);

    assertFalse(wheel.getCandidates().contains(a));
  }

  // ========== get ==========

  @Test
  void getReturnsNullWhenEmpty() {
    assertNull(wheel.get());
  }

  @Test
  void getReturnsFirstSelectedWhenAvailable() {
    Identifier a = id("a");
    Identifier b = id("b");
    registerBoth(0, a, b);
    wheel.selectAt(0, a, b);

    assertEquals(a, wheel.get());
  }

  @Test
  void getReturnsFirstCandidateWhenNoSelected() {
    Identifier a = id("a");
    Identifier b = id("b");
    registry.register(perspective("a"));
    registry.register(perspective("b"));
    wheel.register(a, 1);
    wheel.register(b, 0);

    assertEquals(b, wheel.get());
  }

  @Test
  void getReturnsActiveIdWhenSet() {
    Identifier a = id("a");
    Identifier b = id("b");
    registerBoth(0, a, b);
    wheel.selectAt(0, a, b);

    wheel.cycleForward();
    assertEquals(b, wheel.get());
  }

  // ========== cycleForward ==========

  @Test
  void cycleForwardFromNullSelectsFirst() {
    Identifier a = id("a");
    Identifier b = id("b");
    registerBoth(0, a, b);
    wheel.selectAt(0, a, b);

    wheel.cycleForward();
    assertEquals(b, wheel.get());
  }

  @Test
  void cycleForwardWrapsAround() {
    Identifier a = id("a");
    Identifier b = id("b");
    registerBoth(0, a, b);
    wheel.selectAt(0, a, b);

    wheel.cycleForward();
    wheel.cycleForward();
    assertEquals(a, wheel.get());
  }

  @Test
  void cycleForwardSkipsUnregistered() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registry.register(perspective("a"));
    registry.register(perspective("c"));
    wheel.register(a, 0);
    wheel.register(c, 1);
    // b is NOT registered with wheel
    wheel.selectAt(0, a, b, c);

    // b not in priorities, so ordered = [a, c]
    wheel.cycleForward();
    assertEquals(c, wheel.get());
  }

  @Test
  void cycleForwardEmptyWheelDoesNothing() {
    wheel.cycleForward();
    assertNull(wheel.get());
  }

  // ========== cycleBackward ==========

  @Test
  void cycleForwardWithNoSelectedCyclesCandidates() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registerBoth(0, a, b, c);
    // No selectAt — all are candidates, sorted by priority (all 0), then by id
    // ordered: [a, b, c]

    // get() returns first available = a, cycleForward advances to next = b
    wheel.cycleForward();
    assertEquals(b, wheel.get());
    wheel.cycleForward();
    assertEquals(c, wheel.get());
    wheel.cycleForward();
    assertEquals(a, wheel.get());
    wheel.cycleForward();
    assertEquals(b, wheel.get());
  }

  @Test
  void cycleBackwardFromNullSelectsLast() {
    Identifier a = id("a");
    Identifier b = id("b");
    registerBoth(0, a, b);
    wheel.selectAt(0, a, b);

    wheel.cycleBackward();
    assertEquals(b, wheel.get());
  }

  @Test
  void cycleBackwardWrapsAround() {
    Identifier a = id("a");
    Identifier b = id("b");
    registerBoth(0, a, b);
    wheel.selectAt(0, a, b);

    wheel.cycleForward();
    wheel.cycleBackward();
    assertEquals(a, wheel.get());
  }

  @Test
  void cycleBackwardSkipsUnregistered() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registry.register(perspective("a"));
    registry.register(perspective("c"));
    wheel.register(a, 0);
    wheel.register(c, 1);
    wheel.selectAt(0, a, b, c);

    wheel.cycleForward();
    wheel.cycleBackward();
    assertEquals(a, wheel.get());
  }

  // ========== selected + candidate ordering ==========

  @Test
  void selectedComesBeforeCandidatesInOrder() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registry.register(perspective("a"));
    registry.register(perspective("b"));
    registry.register(perspective("c"));
    wheel.register(a, 0);
    wheel.register(b, 0);
    wheel.register(c, 0);
    wheel.selectAt(0, c);

    List<Identifier> ordered = wheel.getOrdered();
    assertEquals(c, ordered.get(0));
    assertTrue(ordered.indexOf(a) > ordered.indexOf(c));
    assertTrue(ordered.indexOf(b) > ordered.indexOf(c));
  }

  @Test
  void candidatesSortedByPriority() {
    Identifier low = id("low");
    Identifier mid = id("mid");
    Identifier high = id("high");
    registry.register(perspective("low"));
    registry.register(perspective("mid"));
    registry.register(perspective("high"));
    wheel.register(high, 10);
    wheel.register(low, 0);
    wheel.register(mid, 5);

    List<Identifier> candidates = wheel.getCandidates().stream().toList();
    assertEquals(low, candidates.get(0));
    assertEquals(mid, candidates.get(1));
    assertEquals(high, candidates.get(2));
  }

  @Test
  void candidatesSamePrioritySortedById() {
    Identifier alpha = id("alpha");
    Identifier beta = id("beta");
    registry.register(perspective("alpha"));
    registry.register(perspective("beta"));
    wheel.register(beta, 5);
    wheel.register(alpha, 5);

    List<Identifier> candidates = wheel.getCandidates().stream().toList();
    assertEquals(alpha, candidates.get(0));
    assertEquals(beta, candidates.get(1));
  }

  // ========== disable ==========

  @Test
  void disableRemovesFromSelected() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);
    wheel.selectAt(0, a);
    wheel.disable(a);

    assertTrue(wheel.getDisabled().contains(a));
    assertFalse(wheel.getSelected().contains(a));
  }

  @Test
  void disableRemovesFromCandidate() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);
    wheel.disable(a);

    assertTrue(wheel.getDisabled().contains(a));
    assertFalse(wheel.getCandidates().contains(a));
  }

  @Test
  void cycleForwardSkipsDisabled() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registerBoth(0, a, b, c);
    wheel.selectAt(0, a, b, c);

    wheel.disable(b);
    wheel.cycleForward();
    assertEquals(c, wheel.get());
  }

  @Test
  void cycleBackwardSkipsDisabled() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registerBoth(0, a, b, c);
    wheel.selectAt(0, a, b, c);

    wheel.cycleForward();
    wheel.disable(b);
    wheel.cycleBackward();
    assertEquals(c, wheel.get());
  }

  @Test
  void disableAlreadyDisabledDoesNotDuplicate() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);
    wheel.disable(a);
    int sizeBefore = wheel.getDisabled().size();

    wheel.disable(a);

    assertEquals(sizeBefore, wheel.getDisabled().size());
  }

  @Test
  void selectedCandidateDisabledAreMutuallyExclusive() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registerBoth(0, a, b, c);
    wheel.selectAt(0, a);
    wheel.disable(b);

    // a is selected, b is disabled, c is candidate
    assertTrue(wheel.getSelected().contains(a));
    assertFalse(wheel.getCandidates().contains(a));
    assertFalse(wheel.getDisabled().contains(a));

    assertTrue(wheel.getDisabled().contains(b));
    assertFalse(wheel.getSelected().contains(b));
    assertFalse(wheel.getCandidates().contains(b));

    assertTrue(wheel.getCandidates().contains(c));
    assertFalse(wheel.getSelected().contains(c));
    assertFalse(wheel.getDisabled().contains(c));
  }

  // ========== selectAt ==========

  @Test
  void selectAtMovesFromCandidateToSelected() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);

    wheel.selectAt(0, a);

    assertTrue(wheel.getSelected().contains(a));
    assertFalse(wheel.getCandidates().contains(a));
  }

  @Test
  void selectAtReordersWithinSelected() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registerBoth(0, a, b, c);
    wheel.selectAt(0, a, b, c);

    wheel.selectAt(0, c);

    assertEquals(c, wheel.getSelected().get(0));
    assertEquals(a, wheel.getSelected().get(1));
  }

  // ========== deselect ==========

  @Test
  void deselectMovesFromSelectedToCandidate() {
    Identifier a = id("a");
    registry.register(perspective("a"));
    wheel.register(a, 0);
    wheel.selectAt(0, a);

    wheel.deselect(a);

    assertFalse(wheel.getSelected().contains(a));
    assertTrue(wheel.getCandidates().contains(a));
  }

  // ========== setSelected ==========

  @Test
  void setSelectedReplacesSelectedList() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registerBoth(0, a, b, c);
    wheel.selectAt(0, a, b);

    wheel.setSelected(List.of(c, a));

    assertEquals(2, wheel.getSelected().size());
    assertEquals(c, wheel.getSelected().get(0));
    assertEquals(a, wheel.getSelected().get(1));
    assertTrue(wheel.getCandidates().contains(b));
  }

  @Test
  void setSelectedIgnoresUnregisteredIds() {
    Identifier a = id("a");
    Identifier b = id("b");
    registry.register(perspective("a"));
    wheel.register(a, 0);

    wheel.setSelected(List.of(a, b));

    assertEquals(1, wheel.getSelected().size());
    assertEquals(a, wheel.getSelected().get(0));
  }

  // ========== restore ==========

  @Test
  void restoreSetsStateCorrectly() {
    Identifier a = id("a");
    Identifier b = id("b");
    registerBoth(0, a, b);

    wheel.restore(a, List.of(a, b), java.util.Set.of());

    assertEquals(a, wheel.get());
    assertEquals(2, wheel.getSelected().size());
  }

  // ========== F5 cycling through both selected and candidate ==========

  @Test
  void f5CyclesThroughSelectedThenCandidate() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    registry.register(perspective("a"));
    registry.register(perspective("b"));
    registry.register(perspective("c"));
    wheel.register(a, 2);
    wheel.register(b, 0);
    wheel.register(c, 1);
    wheel.selectAt(0, a);

    // ordered: [a, b, c] — a is selected, b(0) and c(1) are candidates
    wheel.cycleForward();
    assertEquals(b, wheel.get());
    wheel.cycleForward();
    assertEquals(c, wheel.get());
    wheel.cycleForward();
    assertEquals(a, wheel.get());
  }

  // ========== getOrdered skips unregistered ==========

  @Test
  void getOrderedSkipsIdsNotInPriorities() {
    Identifier a = id("a");
    Identifier b = id("b");
    registry.register(perspective("a"));
    registry.register(perspective("b"));
    wheel.register(a, 0);
    wheel.selectAt(0, a, b);

    // b was selectAt'd but never register'd — should be excluded
    List<Identifier> ordered = wheel.getOrdered();
    assertEquals(1, ordered.size());
    assertEquals(a, ordered.get(0));
  }

  // ========== registry check ==========

  @Test
  void getReturnsNullWhenNotInRegistry() {
    Identifier a = id("a");
    // Register with wheel but NOT with registry
    wheel.register(a, 0);

    assertNull(wheel.get());
  }

  @Test
  void cycleForwardSkipsNotInRegistry() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    // a and c in registry, b only in wheel
    registry.register(perspective("a"));
    registry.register(perspective("c"));
    wheel.register(a, 0);
    wheel.register(b, 1);
    wheel.register(c, 2);
    wheel.selectAt(0, a, b, c);

    // ordered: [a, b, c], but b is not in registry so isAvailable returns false
    wheel.cycleForward(); // skip b -> c
    assertEquals(c, wheel.get());
  }

  // ========== isAvailable ==========

  @Test
  void getSkipsUnavailablePerspective() {
    MutablePerspective a = mutablePerspective("a", true);
    MutablePerspective b = mutablePerspective("b", false);
    registerMutable(0, a);
    registerMutable(1, b);

    // b is unavailable, get should return a
    assertEquals(a.id(), wheel.get());
  }

  @Test
  void getReturnsNullWhenAllUnavailable() {
    MutablePerspective a = mutablePerspective("a", false);
    registerMutable(0, a);

    assertNull(wheel.get());
  }

  @Test
  void getSkipsUnavailableActiveId() {
    MutablePerspective a = mutablePerspective("a", true);
    MutablePerspective b = mutablePerspective("b", true);
    MutablePerspective c = mutablePerspective("c", true);
    registerMutable(0, a);
    registerMutable(1, b);
    registerMutable(2, c);
    wheel.selectAt(0, a.id(), b.id(), c.id());

    // Set activeId to a
    wheel.cycleForward(); // -> b (a is current, advance to b)
    assertEquals(b.id(), wheel.get());

    // Now b becomes unavailable — get() should skip it
    b.available[0] = false;
    assertEquals(c.id(), wheel.get());
  }

  @Test
  void cycleForwardSkipsUnavailable() {
    MutablePerspective a = mutablePerspective("a", true);
    MutablePerspective b = mutablePerspective("b", false);
    MutablePerspective c = mutablePerspective("c", true);
    registerMutable(0, a);
    registerMutable(1, b);
    registerMutable(2, c);
    wheel.selectAt(0, a.id(), b.id(), c.id());

    // ordered: [a, b, c], b unavailable
    wheel.cycleForward(); // skip b -> c
    assertEquals(c.id(), wheel.get());
  }

  @Test
  void cycleBackwardSkipsUnavailable() {
    MutablePerspective a = mutablePerspective("a", true);
    MutablePerspective b = mutablePerspective("b", false);
    MutablePerspective c = mutablePerspective("c", true);
    registerMutable(0, a);
    registerMutable(1, b);
    registerMutable(2, c);
    wheel.selectAt(0, a.id(), b.id(), c.id());

    wheel.cycleForward(); // -> c
    wheel.cycleBackward(); // skip b -> a
    assertEquals(a.id(), wheel.get());
  }
}
