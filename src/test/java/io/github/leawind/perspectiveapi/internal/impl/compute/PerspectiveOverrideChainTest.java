package io.github.leawind.perspectiveapi.internal.impl.compute;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.perspectiveapi.api.compute.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
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
    return Bridge.createIdentifier("test", path);
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

    Identifier resolved = chain.get();
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

  // ========== computeId ==========

  @Test
  void computeIdEmptyChainReturnsNull() {
    assertNull(chain.get());
  }

  @Test
  void computeIdSingleEntry() {
    Identifier key = id("a");
    chain.push(key, 10, () -> key);
    assertEquals(key, chain.get());
  }

  @Test
  void computeIdReturnsHighestPriorityFirst() {
    Identifier low = id("low");
    Identifier high = id("high");
    chain.push(low, 1, () -> low);
    chain.push(high, 100, () -> high);

    assertEquals(high, chain.get());
  }

  @Test
  void computeIdSkipsNullSupplier() {
    Identifier fallback = id("fallback");
    chain.push(id("null_supplier"), 100, () -> null);
    chain.push(fallback, 1, () -> fallback);

    assertEquals(fallback, chain.get());
  }

  @Test
  void computeIdSkipsFailingValidator() {
    Identifier fallback = id("fallback");
    chain.push(id("invalid"), 100, () -> id("invalid"));
    chain.push(fallback, 1, () -> fallback);

    chain.setValidator(id -> !"invalid".equals(id.getPath()));
    assertEquals(fallback, chain.get());
  }

  @Test
  void computeIdAllFailReturnsNull() {
    chain.push(id("a"), 10, () -> null);
    chain.push(id("b"), 5, () -> null);
    assertNull(chain.get());
  }

  @Test
  void computeIdValidatorRejectsAll() {
    chain.push(id("a"), 10, () -> id("a"));
    chain.push(id("b"), 5, () -> id("b"));
    chain.setValidator(id -> false);
    assertNull(chain.get());
  }

  @Test
  void computeIdPriorityOrdering() {
    Identifier first = id("first");
    Identifier second = id("second");
    Identifier third = id("third");
    chain.push(third, 1, () -> third);
    chain.push(first, 100, () -> first);
    chain.push(second, 50, () -> second);

    assertEquals(first, chain.get());

    chain.pop(first);
    assertEquals(second, chain.get());

    chain.pop(second);
    assertEquals(third, chain.get());
  }

  @Test
  void computeIdAfterPop() {
    Identifier a = id("a");
    Identifier b = id("b");
    chain.push(a, 10, () -> a);
    chain.push(b, 5, () -> b);

    chain.pop(a);
    assertEquals(b, chain.get());
  }

  @Test
  void computeIdAfterClear() {
    chain.push(id("a"), 10, () -> id("a"));
    chain.clear();
    assertNull(chain.get());
  }

  @Test
  void pushSamePriorityMaintainsInsertionOrder() {
    Identifier first = id("first");
    Identifier second = id("second");
    chain.push(first, 10, () -> first);
    chain.push(second, 10, () -> second);

    // Both have same priority; first pushed should be evaluated first
    assertEquals(first, chain.get());
  }

  // ========== setValidator ==========

  @Test
  void setValidatorOverridesValidator() {
    Identifier a = id("a");
    Identifier b = id("b");
    chain.push(a, 10, () -> a);
    chain.push(b, 5, () -> b);

    // Initially no validator, returns highest priority
    assertEquals(a, chain.get());

    // Set validator that rejects 'a'
    chain.setValidator(id -> !"a".equals(id.getPath()));
    assertEquals(b, chain.get());
  }

  @Test
  void setValidatorWithNullSupplier() {
    Identifier fallback = id("fallback");
    chain.push(id("null_supplier"), 100, () -> null);
    chain.push(fallback, 1, () -> fallback);

    chain.setValidator(id -> !"fallback".equals(id.getPath()));
    assertNull(chain.get());
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
