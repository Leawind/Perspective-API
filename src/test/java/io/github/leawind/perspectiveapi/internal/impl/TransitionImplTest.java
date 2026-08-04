package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.ProjectionMode;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import org.joml.Quaternionf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransitionImplTest {
  private TransitionImpl transition;
  private PerspectiveStateImpl start;
  private PerspectiveStateImpl target;

  @BeforeEach
  void beforeEach() {
    transition = new TransitionImpl();
    transition.setDurationMs(100.0);
    start = state(0.0, 0.0f, 60.0f);
    target = state(10.0, 90.0f, 100.0f);
    transition.setStartState(1_000.0, start);
  }

  @Test
  void exposesCommonDefaultAndUsesIndependentAlgorithmInstances() {
    TransitionImpl first = new TransitionImpl();
    TransitionImpl second = new TransitionImpl();

    assertEquals(TransitionImpl.DEFAULT_DURATION_MS, first.getDurationMs());
    assertNotSame(first.algorithm(), second.algorithm());
  }

  @Test
  void rejectsInvalidDuration() {
    assertThrows(IllegalArgumentException.class, () -> transition.setDurationMs(-1.0));
    assertThrows(IllegalArgumentException.class, () -> transition.setDurationMs(Double.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> transition.setDurationMs(Double.POSITIVE_INFINITY));
  }

  @Test
  void transitionBoundaryUsesDuration() {
    assertTrue(transition.isInTransition(1_000.0));
    assertTrue(transition.isInTransition(1_099.999));
    assertFalse(transition.isInTransition(1_100.0));
  }

  @Test
  void usesTargetAfterTransition() {
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    transition.update(1_200.0, target, dest);

    assertStateEquals(target, dest);
  }

  @Test
  void zeroDurationUsesTargetImmediately() {
    transition.setDurationMs(0.0);
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    transition.update(1_000.0, target, dest);

    assertStateEquals(target, dest);
  }

  @Test
  void switchesProjectionModeDiscretely() {
    start.setProjectionMode(ProjectionMode.PERSPECTIVE);
    target.setProjectionMode(ProjectionMode.ORTHOGRAPHIC);
    transition.setStartState(1_000.0, start);
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    transition.update(1_001.0, target, dest);

    assertEquals(ProjectionMode.ORTHOGRAPHIC, dest.projectionMode());
  }

  @Test
  void completedUpdateSupportsAliasingTargetAndDestination() {
    transition.update(1_100.0, target, target);

    assertStateEquals(state(10.0, 90.0f, 100.0f), target);
  }

  private static void assertStateEquals(PerspectiveStateImpl expected, PerspectiveStateImpl actual) {
    TestUtils.assertVectorEquals(expected.position(), actual.position());
    TestUtils.assertQuatEquals(expected.rotation(), actual.rotation());
    assertEquals(expected.getFovDeg(), actual.getFovDeg());
    assertEquals(expected.projectionMode(), actual.projectionMode());
    assertEquals(expected.getOrthographicHeight(), actual.getOrthographicHeight());
  }

  private static PerspectiveStateImpl state(double x, float yawDeg, float fovDeg) {
    PerspectiveStateImpl state = new PerspectiveStateImpl();
    state.position().set(x, 0.0, 0.0);
    state.rotation().set(rotation(yawDeg));
    state.setFovDeg(fovDeg);
    return state;
  }

  private static Quaternionf rotation(float yawDeg) {
    return new Quaternionf().rotationY((float) Math.toRadians(yawDeg));
  }
}
