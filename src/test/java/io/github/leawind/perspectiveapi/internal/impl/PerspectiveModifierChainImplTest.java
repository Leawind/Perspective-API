package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.perspectiveapi.api.PerspectiveModifier;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveContextImpl;
import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerspectiveModifierChainImplTest {
  private PerspectiveModifierChainImpl chain;
  private PerspectiveStateImpl state;
  private PerspectiveContext context;

  @BeforeEach
  void beforeEach() {
    chain =
        new PerspectiveModifierChainImpl(
            new ThrottledPerspectiveSanitizer(new Sanitizer.ThrottledAction(0)));
    state = new PerspectiveStateImpl();
    context = new PerspectiveContextImpl();
  }

  @Test
  void appliesModifiersByAscendingPriority() {
    chain.register("last", 20, modifier(s -> appendDigit(s, 3)));
    chain.register("first", -10, modifier(s -> appendDigit(s, 1)));
    chain.register("middle", 0, modifier(s -> appendDigit(s, 2)));

    chain.applyCameraState(state, context);

    assertEquals(123.0, state.position().x);
  }

  @Test
  void equalPriorityUsesRegistrationOrderAndReplacementMovesToEnd() {
    chain.register("first", 0, modifier(s -> appendDigit(s, 1)));
    chain.register("second", 0, modifier(s -> appendDigit(s, 2)));

    chain.applyCameraState(state, context);
    assertEquals(12.0, state.position().x);

    state.position().zero();
    chain.register("first", 0, modifier(s -> appendDigit(s, 1)));
    chain.applyCameraState(state, context);
    assertEquals(21.0, state.position().x);
  }

  @Test
  void unregisterRemovesOnlyMatchingEntry() {
    chain.register("first", 0, modifier(s -> appendDigit(s, 1)));
    chain.register("second", 0, modifier(s -> appendDigit(s, 2)));

    chain.unregister("first");
    chain.unregister("missing");
    chain.applyCameraState(state, context);

    assertEquals(2.0, state.position().x);
  }

  @Test
  void unavailableAndFailingAvailabilityChecksAreSkipped() {
    AtomicInteger applications = new AtomicInteger();
    chain.register("unavailable", 0, conditionalModifier(false, applications));
    chain.register("failure", 1, throwingAvailabilityModifier(applications));
    chain.register("available", 2, conditionalModifier(true, applications));

    chain.applyCameraState(state, context);

    assertEquals(1, applications.get());
  }

  @Test
  void failedModifierIsFullyRevertedAndDoesNotStopLaterModifiers() {
    state.position().set(1.0, 2.0, 3.0);
    state.rotation().rotationY(0.25f);
    state.setFovDeg(70.0f);
    chain.register(
        "failure",
        0,
        modifier(
            s -> {
              s.position().set(9.0, 9.0, 9.0);
              s.rotation().identity();
              s.setFovDeg(120.0f);
              throw new IllegalStateException("failure");
            }));
    chain.register("after", 1, modifier(s -> s.position().add(1.0, 0.0, 0.0)));

    chain.applyCameraState(state, context);

    TestUtils.assertVectorEquals(new Vector3d(2.0, 2.0, 3.0), state.position());
    TestUtils.assertQuatEquals(new Quaternionf().rotationY(0.25f), state.rotation());
    assertEquals(70.0f, state.getFovDeg());
  }

  @Test
  void invalidFieldsAreIndividuallyReverted() {
    state.position().set(1.0, 2.0, 3.0);
    state.rotation().rotationY(0.25f);
    state.setFovDeg(70.0f);
    chain.register(
        "invalid",
        0,
        modifier(
            s -> {
              s.position().x = Double.NaN;
              s.rotation().rotationX(0.5f);
              s.setFovDeg(100.0f);
            }));

    chain.applyCameraState(state, context);

    TestUtils.assertVectorEquals(new Vector3d(1.0, 2.0, 3.0), state.position());
    TestUtils.assertQuatEquals(new Quaternionf().rotationX(0.5f), state.rotation());
    assertEquals(100.0f, state.getFovDeg());
  }

  @Test
  void rejectsNullRegistrationArguments() {
    PerspectiveModifier modifier = modifier(s -> {});

    assertThrows(NullPointerException.class, () -> chain.register(null, 0, modifier));
    assertThrows(NullPointerException.class, () -> chain.register("test", 0, null));
    assertThrows(NullPointerException.class, () -> chain.unregister(null));
  }

  private static PerspectiveModifier modifier(Consumer<PerspectiveState.Mutable> action) {
    return new PerspectiveModifier() {
      @Override
      public void apply(
          PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext context) {
        action.accept(state);
      }
    };
  }

  private static PerspectiveModifier conditionalModifier(
      boolean available, AtomicInteger applications) {
    return new PerspectiveModifier() {
      @Override
      public boolean isAvailable() {
        return available;
      }

      @Override
      public void apply(
          PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext context) {
        applications.incrementAndGet();
      }
    };
  }

  private static PerspectiveModifier throwingAvailabilityModifier(AtomicInteger applications) {
    return new PerspectiveModifier() {
      @Override
      public boolean isAvailable() {
        throw new IllegalStateException("failure");
      }

      @Override
      public void apply(
          PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext context) {
        applications.incrementAndGet();
      }
    };
  }

  private static void appendDigit(PerspectiveState.Mutable state, int digit) {
    state.position().x = state.position().x * 10.0 + digit;
  }
}
