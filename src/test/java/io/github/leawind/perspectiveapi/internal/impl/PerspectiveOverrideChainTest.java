package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerspectiveOverrideChainTest {

  private PerspectiveOverrideChainImpl chain;

  private static PerspectiveRegistry testRegistry() {
    return new PerspectiveRegistry() {
      @Override
      public boolean contains(@Nullable String id) {
        return id != null && id.startsWith("test.");
      }

      @Override
      public @Nullable Perspective get(@NonNull String id) {
        if (!contains(id)) return null;
        return new Perspective() {
          @Override
          public @NonNull String id() {
            return id;
          }

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
          public int priority() {
            return 0;
          }

          @Override
          public @Nullable Identifier icon() {
            return null;
          }

          @Override
          public boolean isAvailable() {
            return !id.endsWith("unavailable");
          }
        };
      }
    };
  }

  @BeforeEach
  void beforeEach() {
    chain = new PerspectiveOverrideChainImpl(testRegistry());
  }

  private static String id(String path) {
    return "test." + path;
  }

  // ========== push / has ==========

  @Test
  void pushAndHas() {
    String key = id("a");
    assertFalse(chain.contains(key));
    chain.register(key, 10, () -> "test.a");
    assertTrue(chain.contains(key));
  }

  @Test
  void pushReplacesSameKey() {
    String key = id("a");
    chain.register(key, 10, () -> "test.a");
    chain.register(key, 20, () -> "test.b");

    String resolved = chain.get();
    assertEquals("test.b", resolved);
  }

  // ========== pop ==========

  @Test
  void popRemovesEntry() {
    String key = id("a");
    chain.register(key, 10, () -> "test.a");
    assertTrue(chain.contains(key));
    chain.unregister(key);
    assertFalse(chain.contains(key));
  }

  @Test
  void popNonExistentKeyDoesNothing() {
    chain.unregister(id("nope"));
    // should not throw
  }

  // ========== clear ==========

  @Test
  void clearRemovesAllEntries() {
    chain.register(id("a"), 10, () -> "test.a");
    chain.register(id("b"), 5, () -> "test.b");
    chain.clear();
    assertFalse(chain.contains(id("a")));
    assertFalse(chain.contains(id("b")));
  }

  // ========== clearExcept ==========

  @Test
  void clearExceptKeepsSpecifiedKeys() {
    String a = id("a");
    String b = id("b");
    String c = id("c");
    chain.register(a, 10, () -> "test.a");
    chain.register(b, 5, () -> "test.b");
    chain.register(c, 1, () -> "test.c");

    chain.clearExcept(a, c);

    assertTrue(chain.contains(a));
    assertFalse(chain.contains(b));
    assertTrue(chain.contains(c));
  }

  // ========== computeId ==========

  @Test
  void computeIdEmptyChainReturnsNull() {
    assertNull(chain.get());
  }

  @Test
  void computeIdSingleEntry() {
    String key = id("a");
    chain.register(key, 10, () -> "test.a");
    assertEquals("test.a", chain.get());
  }

  @Test
  void computeIdReturnsHighestPriorityFirst() {
    String low = id("low");
    String high = id("high");
    chain.register(low, 1, () -> "test.low");
    chain.register(high, 100, () -> "test.high");

    assertEquals("test.high", chain.get());
  }

  @Test
  void computeIdSkipsNullSupplier() {
    String fallback = id("fallback");
    chain.register(id("null_supplier"), 100, () -> null);
    chain.register(fallback, 1, () -> "test.fallback");

    assertEquals("test.fallback", chain.get());
  }

  @Test
  void computeIdAllFailReturnsNull() {
    chain.register(id("a"), 10, () -> null);
    chain.register(id("b"), 5, () -> null);
    assertNull(chain.get());
  }

  @Test
  void computeIdPriorityOrdering() {
    String first = id("first");
    String second = id("second");
    String third = id("third");
    chain.register(third, 1, () -> "test.third");
    chain.register(first, 100, () -> "test.first");
    chain.register(second, 50, () -> "test.second");

    assertEquals("test.first", chain.get());

    chain.unregister(first);
    assertEquals("test.second", chain.get());

    chain.unregister(second);
    assertEquals("test.third", chain.get());
  }

  @Test
  void computeIdAfterPop() {
    String a = id("a");
    String b = id("b");
    chain.register(a, 10, () -> "test.a");
    chain.register(b, 5, () -> "test.b");

    chain.unregister(a);
    assertEquals("test.b", chain.get());
  }

  @Test
  void computeIdAfterClear() {
    chain.register(id("a"), 10, () -> "test.a");
    chain.clear();
    assertNull(chain.get());
  }

  @Test
  void pushSamePriorityMaintainsInsertionOrder() {
    String first = id("first");
    String second = id("second");
    chain.register(first, 10, () -> "test.first");
    chain.register(second, 10, () -> "test.second");

    // Both have same priority; first pushed should be evaluated first
    assertEquals("test.first", chain.get());
  }

  @Test
  void replacingSamePriorityEntryMovesItToEnd() {
    String first = id("first");
    String second = id("second");
    chain.register(first, 10, () -> "test.first");
    chain.register(second, 10, () -> "test.second");
    chain.register(first, 10, () -> null);

    assertEquals("test.second", chain.get());
  }

  @Test
  void skipsUnregisteredAndUnavailableCandidates() {
    chain.register(id("missing"), 30, () -> "missing.id");
    chain.register(id("unavailable"), 20, () -> "test.unavailable");
    chain.register(id("available"), 10, () -> "test.available");

    assertEquals("test.available", chain.get());
  }

  @Test
  void supplierFailureDoesNotStopFallbackResolution() {
    chain.register(
        id("failure"),
        20,
        () -> {
          throw new IllegalStateException("failure");
        });
    chain.register(id("fallback"), 10, () -> "test.fallback");

    assertEquals("test.fallback", chain.get());
  }

  // ========== null safety ==========

  @Test
  void pushNullKeyThrows() {
    assertThrows(NullPointerException.class, () -> chain.register(null, 10, () -> null));
  }

  @Test
  void pushNullSupplierThrows() {
    assertThrows(NullPointerException.class, () -> chain.register(id("a"), 10, null));
  }

  @Test
  void popNullKeyThrows() {
    assertThrows(NullPointerException.class, () -> chain.unregister(null));
  }

  @Test
  void hasNullKeyThrows() {
    assertThrows(NullPointerException.class, () -> chain.contains(null));
  }

  @Test
  void clearExceptNullArrayThrows() {
    assertThrows(NullPointerException.class, () -> chain.clearExcept((String[]) null));
  }
}
