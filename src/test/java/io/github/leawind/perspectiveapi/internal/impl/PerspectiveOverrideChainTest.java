package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerspectiveOverrideChainTest {

  private PerspectiveOverrideChainImpl chain;

  private static PerspectiveRegistry testRegistry() {
    return new PerspectiveRegistry() {
      private final SimpleEventEmitter.Owned<Void> onUpdate = SimpleEventEmitter.create();

      @Override
      public boolean contains(@Nullable String id) {
        return id != null && id.startsWith("test.");
      }

      @Override
      public @Nullable Perspective get(@NonNull String id) {
        return null;
      }

      @Override
      public @NonNull List<Perspective> getAll() {
        return List.of();
      }

      @Override
      public @NonNull SimpleEventEmitter<Void> onUpdate() {
        return onUpdate;
      }
    };
  }

  @BeforeEach
  void beforeEach() {
    chain = new PerspectiveOverrideChainImpl(testRegistry());
  }

  private static Identifier id(String path) {
    return Bridge.createIdentifier("test", path);
  }

  // ========== push / has ==========

  @Test
  void pushAndHas() {
    Identifier key = id("a");
    assertFalse(chain.has(key));
    chain.push(key, 10, () -> "test.a");
    assertTrue(chain.has(key));
  }

  @Test
  void pushReplacesSameKey() {
    Identifier key = id("a");
    chain.push(key, 10, () -> "test.a");
    chain.push(key, 20, () -> "test.b");

    String resolved = chain.get();
    assertEquals("test.b", resolved);
  }

  // ========== pop ==========

  @Test
  void popRemovesEntry() {
    Identifier key = id("a");
    chain.push(key, 10, () -> "test.a");
    assertTrue(chain.has(key));
    chain.pop(key);
    assertFalse(chain.has(key));
  }

  @Test
  void popNonExistentKeyDoesNothing() {
    chain.pop(id("nope"));
    // should not throw
  }

  // ========== clear ==========

  @Test
  void clearRemovesAllEntries() {
    chain.push(id("a"), 10, () -> "test.a");
    chain.push(id("b"), 5, () -> "test.b");
    chain.clear();
    assertFalse(chain.has(id("a")));
    assertFalse(chain.has(id("b")));
  }

  // ========== clearExcept ==========

  @Test
  void clearExceptKeepsSpecifiedKeys() {
    Identifier a = id("a");
    Identifier b = id("b");
    Identifier c = id("c");
    chain.push(a, 10, () -> "test.a");
    chain.push(b, 5, () -> "test.b");
    chain.push(c, 1, () -> "test.c");

    chain.clearExcept(a, c);

    assertTrue(chain.has(a));
    assertFalse(chain.has(b));
    assertTrue(chain.has(c));
  }

  // ========== computeId ==========

  @Test
  void computeIdEmptyChainReturnsNull() {
    assertNull(chain.get());
  }

  @Test
  void computeIdSingleEntry() {
    Identifier key = id("a");
    chain.push(key, 10, () -> "test.a");
    assertEquals("test.a", chain.get());
  }

  @Test
  void computeIdReturnsHighestPriorityFirst() {
    Identifier low = id("low");
    Identifier high = id("high");
    chain.push(low, 1, () -> "test.low");
    chain.push(high, 100, () -> "test.high");

    assertEquals("test.high", chain.get());
  }

  @Test
  void computeIdSkipsNullSupplier() {
    Identifier fallback = id("fallback");
    chain.push(id("null_supplier"), 100, () -> null);
    chain.push(fallback, 1, () -> "test.fallback");

    assertEquals("test.fallback", chain.get());
  }

  @Test
  void computeIdAllFailReturnsNull() {
    chain.push(id("a"), 10, () -> null);
    chain.push(id("b"), 5, () -> null);
    assertNull(chain.get());
  }

  @Test
  void computeIdPriorityOrdering() {
    Identifier first = id("first");
    Identifier second = id("second");
    Identifier third = id("third");
    chain.push(third, 1, () -> "test.third");
    chain.push(first, 100, () -> "test.first");
    chain.push(second, 50, () -> "test.second");

    assertEquals("test.first", chain.get());

    chain.pop(first);
    assertEquals("test.second", chain.get());

    chain.pop(second);
    assertEquals("test.third", chain.get());
  }

  @Test
  void computeIdAfterPop() {
    Identifier a = id("a");
    Identifier b = id("b");
    chain.push(a, 10, () -> "test.a");
    chain.push(b, 5, () -> "test.b");

    chain.pop(a);
    assertEquals("test.b", chain.get());
  }

  @Test
  void computeIdAfterClear() {
    chain.push(id("a"), 10, () -> "test.a");
    chain.clear();
    assertNull(chain.get());
  }

  @Test
  void pushSamePriorityMaintainsInsertionOrder() {
    Identifier first = id("first");
    Identifier second = id("second");
    chain.push(first, 10, () -> "test.first");
    chain.push(second, 10, () -> "test.second");

    // Both have same priority; first pushed should be evaluated first
    assertEquals("test.first", chain.get());
  }

  // ========== null safety ==========

  @Test
  void pushNullKeyThrows() {
    assertThrows(NullPointerException.class, () -> chain.push(null, 10, () -> null));
  }

  @Test
  void pushNullSupplierThrows() {
    assertThrows(NullPointerException.class, () -> chain.push(id("a"), 10, null));
  }

  @Test
  void popNullKeyThrows() {
    assertThrows(NullPointerException.class, () -> chain.pop(null));
  }

  @Test
  void hasNullKeyThrows() {
    assertThrows(NullPointerException.class, () -> chain.has(null));
  }
}
