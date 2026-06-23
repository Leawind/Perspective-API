package io.github.leawind.perspectiveapi.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveOverrideChainImpl;
import io.github.leawind.perspectiveapi.platform.api.Services;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerspectiveOverrideChainTest {

  private PerspectiveOverrideChain chain;

  @BeforeEach
  void beforeEach() {
    chain = new PerspectiveOverrideChainImpl();
  }

  private static Identifier id(String path) {
    return Services.PLATFORM_HELPER.createIdentifier("test", path);
  }

  // ========== push / has ==========

  @Test
  void pushAndHas() {
    Identifier key = id("a");
    assertFalse(chain.has(key));
    chain.push(key, 10, () -> key);
    assertTrue(chain.has(key));
  }

  @Test
  void pushReplacesSameKey() {
    Identifier key = id("a");
    Identifier result = id("b");
    chain.push(key, 10, () -> key);
    chain.push(key, 20, () -> result);

    Identifier resolved = chain.resolve(id -> true);
    assertEquals(result, resolved);
  }

  // ========== pop ==========

  @Test
  void popRemovesEntry() {
    Identifier key = id("a");
    chain.push(key, 10, () -> key);
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
    chain.push(id("a"), 10, () -> id("a"));
    chain.push(id("b"), 5, () -> id("b"));
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
    chain.push(a, 10, () -> a);
    chain.push(b, 5, () -> b);
    chain.push(c, 1, () -> c);

    chain.clearExcept(a, c);

    assertTrue(chain.has(a));
    assertFalse(chain.has(b));
    assertTrue(chain.has(c));
  }

  // ========== resolve ==========

  @Test
  void resolveEmptyChainReturnsNull() {
    assertNull(chain.resolve(id -> true));
  }

  @Test
  void resolveSingleEntry() {
    Identifier key = id("a");
    chain.push(key, 10, () -> key);
    assertEquals(key, chain.resolve(id -> true));
  }

  @Test
  void resolveReturnsHighestPriorityFirst() {
    Identifier low = id("low");
    Identifier high = id("high");
    chain.push(low, 1, () -> low);
    chain.push(high, 100, () -> high);

    assertEquals(high, chain.resolve(id -> true));
  }

  @Test
  void resolveSkipsNullSupplier() {
    Identifier fallback = id("fallback");
    chain.push(id("null_supplier"), 100, () -> null);
    chain.push(fallback, 1, () -> fallback);

    assertEquals(fallback, chain.resolve(id -> true));
  }

  @Test
  void resolveSkipsFailingValidator() {
    Identifier fallback = id("fallback");
    chain.push(id("invalid"), 100, () -> id("invalid"));
    chain.push(fallback, 1, () -> fallback);

    assertEquals(fallback, chain.resolve(id -> !"invalid".equals(id.getPath())));
  }

  @Test
  void resolveAllFailReturnsNull() {
    chain.push(id("a"), 10, () -> null);
    chain.push(id("b"), 5, () -> null);
    assertNull(chain.resolve(id -> true));
  }

  @Test
  void resolveValidatorRejectsAll() {
    chain.push(id("a"), 10, () -> id("a"));
    chain.push(id("b"), 5, () -> id("b"));
    assertNull(chain.resolve(id -> false));
  }

  @Test
  void resolvePriorityOrdering() {
    Identifier first = id("first");
    Identifier second = id("second");
    Identifier third = id("third");
    chain.push(third, 1, () -> third);
    chain.push(first, 100, () -> first);
    chain.push(second, 50, () -> second);

    assertEquals(first, chain.resolve(id -> true));

    chain.pop(first);
    assertEquals(second, chain.resolve(id -> true));

    chain.pop(second);
    assertEquals(third, chain.resolve(id -> true));
  }

  @Test
  void resolveAfterPop() {
    Identifier a = id("a");
    Identifier b = id("b");
    chain.push(a, 10, () -> a);
    chain.push(b, 5, () -> b);

    chain.pop(a);
    assertEquals(b, chain.resolve(id -> true));
  }

  @Test
  void resolveAfterClear() {
    chain.push(id("a"), 10, () -> id("a"));
    chain.clear();
    assertNull(chain.resolve(id -> true));
  }

  @Test
  void pushSamePriorityMaintainsInsertionOrder() {
    Identifier first = id("first");
    Identifier second = id("second");
    chain.push(first, 10, () -> first);
    chain.push(second, 10, () -> second);

    // Both have same priority; first pushed should be evaluated first
    assertEquals(first, chain.resolve(id -> true));
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
