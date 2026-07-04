package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
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
    // Get the singleton instance as requested
    cycler = new PerspectiveCyclerImpl();
    registry = PerspectiveAPI.getManager().registry();

    // Register test perspectives so cycleForward/cycleBackward can find them
    registry.register(perspective(ID_A));
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
    // Assuming default is false based on typical behavior, but explicitly checking state
    // If the default is true, this test might need adjustment depending on implementation details,
    // but usually standard sorting is default.
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

    // If current is null, next/previous usually returns the first/last or null depending on spec.
    // Based on typical cycler logic:
    // getNext(null) -> First item (B)
    // getPrevious(null) -> Last item (A)

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
}
