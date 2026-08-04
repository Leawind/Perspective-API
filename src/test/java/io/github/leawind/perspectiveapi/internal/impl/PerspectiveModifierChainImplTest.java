package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import io.github.leawind.perspectiveapi.api.PerspectiveModifier;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierRegistration;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.ProjectionMode;
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
    chain.register("test.third", 20, modifier(s -> appendDigit(s, 3)));
    chain.register("test.first", -10, modifier(s -> appendDigit(s, 1)));
    chain.register("test.second", 0, modifier(s -> appendDigit(s, 2)));

    chain.applyCameraState(state, context);

    assertEquals(123.0, state.position().x);
  }

  @Test
  void equalPriorityUsesRegistrationOrder() {
    chain.register("test.first", 0, modifier(s -> appendDigit(s, 1)));
    chain.register("test.second", 0, modifier(s -> appendDigit(s, 2)));

    chain.applyCameraState(state, context);
    assertEquals(12.0, state.position().x);
  }

  @Test
  void unregisterRemovesOnlyMatchingEntry() {
    PerspectiveModifierRegistration first =
        chain.register("test.first", 0, modifier(s -> appendDigit(s, 1)));
    chain.register("test.second", 0, modifier(s -> appendDigit(s, 2)));

    assertTrue(first.unregister());
    assertFalse(first.unregister());
    chain.applyCameraState(state, context);

    assertEquals(2.0, state.position().x);
  }

  @Test
  void unavailableAndFailingAvailabilityChecksAreSkipped() {
    AtomicInteger applications = new AtomicInteger();
    chain.register("test.unavailable", 0, conditionalModifier(false, applications));
    chain.register("test.failing", 1, throwingAvailabilityModifier(applications));
    chain.register("test.available", 2, conditionalModifier(true, applications));

    chain.applyCameraState(state, context);

    assertEquals(1, applications.get());
  }

  @Test
  void failedModifierIsFullyRevertedAndDoesNotStopLaterModifiers() {
    state.position().set(1.0, 2.0, 3.0);
    state.rotation().rotationY(0.25f);
    state.setFovDeg(70.0f);
    state.setProjectionMode(ProjectionMode.ORTHOGRAPHIC);
    state.setOrthographicHeight(16.0f);
    chain.register(
        "test.failing",
        0,
        modifier(
            s -> {
              s.position().set(9.0, 9.0, 9.0);
              s.rotation().identity();
              s.setFovDeg(120.0f);
              s.setProjectionMode(ProjectionMode.PERSPECTIVE);
              s.setOrthographicHeight(40.0f);
              throw new IllegalStateException("failure");
            }));
    chain.register(
        "test.later", 1, modifier(s -> s.position().add(1.0, 0.0, 0.0)));

    chain.applyCameraState(state, context);

    TestUtils.assertVectorEquals(new Vector3d(2.0, 2.0, 3.0), state.position());
    TestUtils.assertQuatEquals(new Quaternionf().rotationY(0.25f), state.rotation());
    assertEquals(70.0f, state.getFovDeg());
    assertEquals(ProjectionMode.ORTHOGRAPHIC, state.projectionMode());
    assertEquals(16.0f, state.getOrthographicHeight());
  }

  @Test
  void invalidFieldsAreIndividuallyReverted() {
    state.position().set(1.0, 2.0, 3.0);
    state.rotation().rotationY(0.25f);
    state.setFovDeg(70.0f);
    state.setOrthographicHeight(16.0f);
    chain.register(
        "test.invalid",
        0,
        modifier(
            s -> {
              s.position().x = Double.NaN;
              s.rotation().rotationX(0.5f);
              s.setFovDeg(100.0f);
              s.setOrthographicHeight(Float.NaN);
            }));

    chain.applyCameraState(state, context);

    TestUtils.assertVectorEquals(new Vector3d(1.0, 2.0, 3.0), state.position());
    TestUtils.assertQuatEquals(new Quaternionf().rotationX(0.5f), state.rotation());
    assertEquals(100.0f, state.getFovDeg());
    assertEquals(16.0f, state.getOrthographicHeight());
  }

  @Test
  void rejectsDuplicateIdsUntilRegistrationIsRemoved() {
    PerspectiveModifier modifier = modifier(s -> {});
    PerspectiveModifierRegistration first = chain.register("test.same", 0, modifier);

    assertThrows(
        IllegalArgumentException.class, () -> chain.register("test.same", 0, modifier));
    assertTrue(first.unregister());
    chain.register("test.same", 0, modifier);
  }

  @Test
  void rejectsInvalidRegistrationArguments() {
    PerspectiveModifier modifier = modifier(s -> {});

    assertThrows(NullPointerException.class, () -> chain.register(null, 0, modifier));
    assertThrows(NullPointerException.class, () -> chain.register("test.id", 0, null));
    assertThrows(IllegalArgumentException.class, () -> chain.register("", 0, modifier));
    assertThrows(NullPointerException.class, () -> chain.applyCameraState(null, context));
    assertThrows(NullPointerException.class, () -> chain.applyCameraState(state, null));
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
