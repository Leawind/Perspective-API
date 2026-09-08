package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideRegistration;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistration;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerspectiveOverrideChainTest {
  private PerspectiveOverrideChainImpl chain;

  private static PerspectiveRegistry testRegistry() {
    return new PerspectiveRegistry() {
      @Override
      public @NonNull PerspectiveRegistration register(
          @NonNull PerspectiveInfo info, @NonNull PerspectiveBehavior behavior) {
        throw new UnsupportedOperationException();
      }

      @Override
      public @NonNull PerspectiveRegistration registerDefault(
          @NonNull PerspectiveInfo info,
          int defaultPriority,
          @NonNull PerspectiveBehavior behavior) {
        throw new UnsupportedOperationException();
      }

      @Override
      public boolean contains(@Nullable String id) {
        return id != null && id.startsWith("test.");
      }

      @Override
      public @Nullable Perspective get(@NonNull String id) {
        if (!contains(id)) return null;
        return new Perspective() {
          private final PerspectiveInfo info =
              PerspectiveInfo.builder(id, Component.literal(id)).build();

          @Override
          public @NonNull PerspectiveInfo info() {
            return info;
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

  @Test
  void emptyChainReturnsNull() {
    assertNull(chain.get());
  }

  @Test
  void resolvesHighestAvailablePriority() {
    chain.register("test.missing", 30, () -> "missing.id");
    chain.register("test.unavailable", 20, () -> "test.unavailable");
    chain.register("test.available", 10, () -> "test.available");

    assertEquals("test.available", chain.get());
  }

  @Test
  void nullAndFailingSuppliersDoNotStopFallbackResolution() {
    chain.register(
        "test.failing",
        30,
        () -> {
          throw new IllegalStateException("failure");
        });
    chain.register("test.skipping", 20, () -> null);
    chain.register("test.fallback", 10, () -> "test.fallback");

    assertEquals("test.fallback", chain.get());
  }

  @Test
  void equalPriorityUsesRegistrationOrder() {
    chain.register("test.first", 10, () -> "test.first");
    chain.register("test.second", 10, () -> "test.second");

    assertEquals("test.first", chain.get());
  }

  @Test
  void registrationHandleRemovesOnlyItsOwnEntry() {
    PerspectiveOverrideRegistration first = chain.register("test.first", 10, () -> "test.first");
    chain.register("test.second", 10, () -> "test.second");

    assertTrue(first.unregister());
    assertFalse(first.unregister());
    assertEquals("test.second", chain.get());
  }

  @Test
  void acceptsMultipleRegistrationsOfSameSupplier() {
    java.util.function.Supplier<String> supplier = () -> "test.same";
    PerspectiveOverrideRegistration first = chain.register("test.same_low", 10, supplier);
    PerspectiveOverrideRegistration second = chain.register("test.same_high", 20, supplier);

    assertTrue(second.unregister());
    assertEquals("test.same", chain.get());
  }

  @Test
  void rejectsInvalidAndDuplicateIds() {
    assertThrows(
        IllegalArgumentException.class, () -> chain.register("", 10, () -> "test.first"));
    chain.register("test.unique", 10, () -> "test.first");
    assertThrows(
        IllegalArgumentException.class,
        () -> chain.register("test.unique", 20, () -> "test.second"));
  }

  @Test
  void rejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> chain.register(null, 10, () -> "test.first"));
    assertThrows(NullPointerException.class, () -> chain.register("test.null_supplier", 10, null));
  }
}
