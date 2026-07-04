package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerspectiveCyclerImplTest {

  private PerspectiveCycler cycler;
  private PerspectiveRegistry registry;

  // Helper identifiers for testing
  private static final Identifier ID_A = Bridge.createIdentifier("test", "a");
  private static final Identifier ID_B = Bridge.createIdentifier("test", "b");
  private static final Identifier ID_C = Bridge.createIdentifier("test", "c");
  private static final Identifier ID_D = Bridge.createIdentifier("test", "d");

  private static Perspective perspective(Identifier id) {
    return new Perspective() {
      @Override
      public @NonNull Identifier id() {
        return id;
      }

      @Override
      public @NonNull CameraType cameraType() {
        return CameraType.FIRST_PERSON;
      }
    };
  }

  @BeforeEach
  void setUp() {
    cycler = new PerspectiveCyclerImpl();
    // Use a fresh registry instead of the singleton
    registry = new PerspectiveRegistryImpl(perspective(ID_A));

    // Register test perspectives so cycleForward/cycleBackward can find them
    registry.register(perspective(ID_B));
    registry.register(perspective(ID_C));
    registry.register(perspective(ID_D));

    // Ensure clean state before each test
    cycler.clear();
  }

  @Test
  void testAddAndList() {
    assertTrue(cycler.getIds().isEmpty());

    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    List<Identifier> list = cycler.getIds();

    // Should be sorted by priority: B(5), A(10), C(20)
    assertEquals(3, list.size());
    assertEquals(ID_B, list.get(0));
    assertEquals(ID_A, list.get(1));
    assertEquals(ID_C, list.get(2));
  }

  @Test
  void testAddReplacesExistingPriority() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);

    // Update A's priority to be lower than B
    cycler.add(ID_A, 1);

    List<Identifier> list = cycler.getIds();
    assertEquals(2, list.size());
    assertEquals(ID_A, list.get(0)); // Priority 1
    assertEquals(ID_B, list.get(1)); // Priority 5
  }

  @Test
  void testRemove() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    assertEquals(3, cycler.getIds().size());

    cycler.remove(ID_B);

    List<Identifier> list = cycler.getIds();
    assertEquals(2, list.size());
    assertEquals(ID_A, list.get(0));
    assertEquals(ID_C, list.get(1));

    // Removing non-existent ID should not throw
    assertDoesNotThrow(() -> cycler.remove(Bridge.createIdentifier("test", "non_existent")));
  }

  @Test
  void testClear() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);

    assertFalse(cycler.getIds().isEmpty());

    cycler.clear();

    assertTrue(cycler.getIds().isEmpty());
    assertNull(cycler.getActiveId());
  }

  @Test
  void testCustomOrderDisabledByDefault() {
    boolean initialUseCustomOrder = cycler.isCustomOrderEnabled();

    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    if (!initialUseCustomOrder) {
      List<Identifier> list = cycler.getIds();
      assertEquals(ID_B, list.get(0));
      assertEquals(ID_A, list.get(1));
      assertEquals(ID_C, list.get(2));
    }
  }

  @Test
  void testSetCustomOrder() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);
    cycler.add(ID_D, 15);

    // Define custom order: C, A, D (B is missing from custom list)
    List<Identifier> customOrder = List.of(ID_C, ID_A, ID_D);
    cycler.setCustomOrder(customOrder);
    cycler.setCustomOrderEnabled(true);

    List<Identifier> list = cycler.getIds();

    // Expected: C, A, D followed by remaining items sorted by priority (B)
    assertEquals(4, list.size());
    assertEquals(ID_C, list.get(0));
    assertEquals(ID_A, list.get(1));
    assertEquals(ID_D, list.get(2));
    assertEquals(ID_B, list.get(3)); // B was not in custom list, so it goes to end
  }

  @Test
  void testToggleCustomOrder() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);

    cycler.setCustomOrderEnabled(false);
    List<Identifier> listStandard = cycler.getIds();
    assertEquals(ID_B, listStandard.get(0));
    assertEquals(ID_A, listStandard.get(1));

    // Set custom order reversed
    cycler.setCustomOrder(List.of(ID_A, ID_B));
    cycler.setCustomOrderEnabled(true);

    List<Identifier> listCustom = cycler.getIds();
    assertEquals(ID_A, listCustom.get(0));
    assertEquals(ID_B, listCustom.get(1));

    // Switch back to standard
    cycler.setCustomOrderEnabled(false);
    List<Identifier> listStandardAgain = cycler.getIds();
    assertEquals(ID_B, listStandardAgain.get(0));
    assertEquals(ID_A, listStandardAgain.get(1));
  }

  @Test
  void testGetNextAndPrevious() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    // Order: B, A, C

    // Test Next
    assertEquals(ID_A, cycler.getNext(ID_B));
    assertEquals(ID_C, cycler.getNext(ID_A));
    assertEquals(ID_B, cycler.getNext(ID_C)); // Wrap around

    // Test Previous
    assertEquals(ID_B, cycler.getPrevious(ID_A));
    assertEquals(ID_A, cycler.getPrevious(ID_C));
    assertEquals(ID_C, cycler.getPrevious(ID_B)); // Wrap around
  }

  @Test
  void testGetNextPreviousWithNull() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);

    assertEquals(ID_B, cycler.getNext(null));
    assertEquals(ID_A, cycler.getPrevious(null));
  }

  @Test
  void testSetActiveAndGetActiveIdId() {
    assertNull(cycler.getActiveId());

    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);

    cycler.setActiveId(ID_A);
    assertEquals(ID_A, cycler.getActiveId());

    cycler.setActiveId(ID_B);
    assertEquals(ID_B, cycler.getActiveId());

    cycler.setActiveId(null);
    assertNull(cycler.getActiveId());
  }

  @Test
  void testCycleForward() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    // Order: B, A, C
    cycler.setActiveId(ID_B);

    cycler.cycleForward(registry);
    assertEquals(ID_A, cycler.getActiveId());

    cycler.cycleForward(registry);
    assertEquals(ID_C, cycler.getActiveId());

    cycler.cycleForward(registry);
    assertEquals(ID_B, cycler.getActiveId()); // Wrap around
  }

  @Test
  void testCycleBackward() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    // Order: B, A, C
    cycler.setActiveId(ID_C);

    cycler.cycleBackward(registry);
    assertEquals(ID_A, cycler.getActiveId());

    cycler.cycleBackward(registry);
    assertEquals(ID_B, cycler.getActiveId());

    cycler.cycleBackward(registry);
    assertEquals(ID_C, cycler.getActiveId()); // Wrap around
  }

  @Test
  void testCycleWithEmptyList() {
    cycler.clear();
    assertNull(cycler.getActiveId());

    // Cycling with empty list should not throw and active should remain null
    assertDoesNotThrow(() -> cycler.cycleForward(registry));
    assertDoesNotThrow(() -> cycler.cycleBackward(registry));
    assertNull(cycler.getActiveId());
  }

  @Test
  void testGetIdsReturnsImmutableList() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);

    List<Identifier> list = cycler.getIds();
    assertThrows(UnsupportedOperationException.class, () -> list.add(ID_C));
    assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
    assertThrows(UnsupportedOperationException.class, () -> list.set(0, ID_C));
    assertThrows(UnsupportedOperationException.class, list::clear);
  }

  @Test
  void testGetIdsReturnsImmutableListWithCustomOrder() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.setCustomOrder(List.of(ID_A, ID_B));
    cycler.setCustomOrderEnabled(true);

    List<Identifier> list = cycler.getIds();
    assertThrows(UnsupportedOperationException.class, () -> list.add(ID_C));
    assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
  }

  @Test
  void testGetIdsIncludesAllIdsRegardlessOfRegistration() {
    // Only register A and B in the registry
    PerspectiveRegistry limitedRegistry = new PerspectiveRegistryImpl(perspective(ID_A));
    limitedRegistry.register(perspective(ID_B));

    // Add A, B, C, D to the cycler
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);
    cycler.add(ID_D, 15);

    // getIds should include all 4 IDs even though only A, B are registered
    List<Identifier> list = cycler.getIds();
    assertEquals(4, list.size());
    assertTrue(list.contains(ID_A));
    assertTrue(list.contains(ID_B));
    assertTrue(list.contains(ID_C));
    assertTrue(list.contains(ID_D));
  }

  @Test
  void testCycleSkipsUnregisteredIds() {
    // Only register A and B
    PerspectiveRegistry limitedRegistry = new PerspectiveRegistryImpl(perspective(ID_A));
    limitedRegistry.register(perspective(ID_B));

    // Add A, B, C, D to cycler
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);
    cycler.add(ID_D, 15);

    // Order by priority: B(5), A(10), D(15), C(20)
    // Cycle should skip D and C since they are not registered
    cycler.setActiveId(ID_B);
    cycler.cycleForward(limitedRegistry);
    assertEquals(ID_A, cycler.getActiveId());

    cycler.cycleForward(limitedRegistry);
    assertEquals(ID_B, cycler.getActiveId()); // wraps, skips D and C
  }

  @Test
  void testSetCustomOrderDoesNotChangeIsCustomOrderEnabled() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);

    // Enable custom order
    cycler.setCustomOrderEnabled(true);
    assertTrue(cycler.isCustomOrderEnabled());

    // Set custom order should not change the enabled state
    cycler.setCustomOrder(List.of(ID_A, ID_B));
    assertTrue(cycler.isCustomOrderEnabled());

    // Disable custom order
    cycler.setCustomOrderEnabled(false);
    assertFalse(cycler.isCustomOrderEnabled());

    // Set custom order should not re-enable it
    cycler.setCustomOrder(List.of(ID_B, ID_A));
    assertFalse(cycler.isCustomOrderEnabled());
  }

  @Test
  void testSetCustomOrderWithIncompleteListAppendsMissingByPriority() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);
    cycler.add(ID_D, 15);

    // Custom order only contains C and A
    // Missing: B(5) and D(15), should be appended sorted by priority
    cycler.setCustomOrder(List.of(ID_C, ID_A));
    cycler.setCustomOrderEnabled(true);

    List<Identifier> list = cycler.getIds();
    assertEquals(4, list.size());
    assertEquals(ID_C, list.get(0));
    assertEquals(ID_A, list.get(1));
    assertEquals(ID_B, list.get(2)); // B has priority 5
    assertEquals(ID_D, list.get(3)); // D has priority 15
  }

  @Test
  void testSetCustomOrderWithEmptyListAppendsAllByPriority() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    // Empty custom order: all IDs should be appended sorted by priority
    cycler.setCustomOrder(List.of());
    cycler.setCustomOrderEnabled(true);

    List<Identifier> list = cycler.getIds();
    assertEquals(3, list.size());
    assertEquals(ID_B, list.get(0)); // priority 5
    assertEquals(ID_A, list.get(1)); // priority 10
    assertEquals(ID_C, list.get(2)); // priority 20
  }

  @Test
  void testSetCustomOrderWithAllIdsNoRemainder() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    // Custom order contains all IDs
    cycler.setCustomOrder(List.of(ID_C, ID_A, ID_B));
    cycler.setCustomOrderEnabled(true);

    List<Identifier> list = cycler.getIds();
    assertEquals(3, list.size());
    assertEquals(ID_C, list.get(0));
    assertEquals(ID_A, list.get(1));
    assertEquals(ID_B, list.get(2));
  }

  @Test
  void testSetCustomOrderIgnoresIdsNotInCycler() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);

    // Custom order includes ID_C which is not in the cycler
    cycler.setCustomOrder(List.of(ID_A, ID_C));
    cycler.setCustomOrderEnabled(true);

    List<Identifier> list = cycler.getIds();
    assertEquals(2, list.size());
    assertEquals(ID_A, list.get(0));
    assertEquals(ID_B, list.get(1)); // ID_C ignored, ID_B appended by priority
  }

  @Test
  void testSetCustomOrderPreservesOrderAcrossMultipleCalls() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    // First call
    cycler.setCustomOrder(List.of(ID_A, ID_B));
    cycler.setCustomOrderEnabled(true);

    List<Identifier> list1 = cycler.getIds();
    assertEquals(ID_A, list1.get(0));
    assertEquals(ID_B, list1.get(1));
    assertEquals(ID_C, list1.get(2));

    // Second call overrides the custom order
    cycler.setCustomOrder(List.of(ID_C, ID_B, ID_A));

    List<Identifier> list2 = cycler.getIds();
    assertEquals(ID_C, list2.get(0));
    assertEquals(ID_B, list2.get(1));
    assertEquals(ID_A, list2.get(2));
  }

  // --- getCustomOrder specific tests ---

  @Test
  void testGetCustomOrderReturnsUnmodifiableList() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);

    List<Identifier> order = cycler.getCustomOrder();
    assertThrows(UnsupportedOperationException.class, () -> order.add(ID_C));
    assertThrows(UnsupportedOperationException.class, () -> order.remove(0));
    assertThrows(UnsupportedOperationException.class, () -> order.set(0, ID_C));
    assertThrows(UnsupportedOperationException.class, order::clear);
  }

  @Test
  void testGetCustomOrderReturnsAllIdsByPriorityWhenNeverSet() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    List<Identifier> order = cycler.getCustomOrder();
    assertEquals(3, order.size());
    assertEquals(ID_B, order.get(0));
    assertEquals(ID_A, order.get(1));
    assertEquals(ID_C, order.get(2));
  }

  @Test
  void testGetCustomOrderAlwaysContainsAllIds() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);
    cycler.add(ID_D, 15);

    // Incomplete custom order: only C and A
    cycler.setCustomOrder(List.of(ID_C, ID_A));

    List<Identifier> order = cycler.getCustomOrder();
    assertEquals(4, order.size());
    assertTrue(order.contains(ID_A));
    assertTrue(order.contains(ID_B));
    assertTrue(order.contains(ID_C));
    assertTrue(order.contains(ID_D));
    // Leading portion follows custom order
    assertEquals(ID_C, order.get(0));
    assertEquals(ID_A, order.get(1));
    // Remainder sorted by priority
    assertEquals(ID_B, order.get(2));
    assertEquals(ID_D, order.get(3));
  }

  @Test
  void testGetCustomOrderReflectsLatestSetCall() {
    cycler.add(ID_A, 10);
    cycler.add(ID_B, 5);
    cycler.add(ID_C, 20);

    cycler.setCustomOrder(List.of(ID_A, ID_B, ID_C));
    assertEquals(ID_A, cycler.getCustomOrder().get(0));

    cycler.setCustomOrder(List.of(ID_C, ID_B, ID_A));
    List<Identifier> order = cycler.getCustomOrder();
    assertEquals(ID_C, order.get(0));
    assertEquals(ID_B, order.get(1));
    assertEquals(ID_A, order.get(2));
  }
}
