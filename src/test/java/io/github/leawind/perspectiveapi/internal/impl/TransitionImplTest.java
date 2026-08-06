package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.ProjectionMode;
import io.github.leawind.perspectiveapi.internal.impl.transition.position.FixedStartPositionTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.rotation.ChasingRotationTransitionAlgorithm;
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
    start.setOrthographicHeight(10.0f);
    target = state(10.0, 90.0f, 100.0f);
    target.setOrthographicHeight(30.0f);
    transition.setStartState(1_000.0, start);
  }

  @Test
  void exposesIndependentDefaultAlgorithms() {
    assertEquals(TransitionImpl.DEFAULT_DURATION_MS, new TransitionImpl().getDurationMs());
    assertEquals(TransitionImpl.DEFAULT_POSITION_ALGORITHM, transition.getPositionAlgorithm());
    assertEquals(TransitionImpl.DEFAULT_ROTATION_ALGORITHM, transition.getRotationAlgorithm());
    assertEquals(TransitionImpl.DEFAULT_FOV_ALGORITHM, transition.getFovAlgorithm());
    assertEquals(
        TransitionImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT_ALGORITHM,
        transition.getOrthographicHeightAlgorithm());
  }

  @Test
  void switchesOnlyTheSelectedChannelAtRuntime() {
    transition.setPositionAlgorithm(FixedStartPositionTransitionAlgorithm.INSTANCE);
    PerspectiveStateImpl actual = new PerspectiveStateImpl();

    transition.update(1_050.0, target, actual);

    assertStateAtProgress(easedProgressAtHalfDuration(), actual);
  }

  @Test
  void switchesEachChannelIndependently() {
    transition.setPositionAlgorithm(FixedStartPositionTransitionAlgorithm.INSTANCE);
    transition.setRotationAlgorithm(ChasingRotationTransitionAlgorithm.INSTANCE);
    PerspectiveStateImpl actual = new PerspectiveStateImpl();

    transition.update(1_050.0, target, actual);

    assertStateAtProgress(easedProgressAtHalfDuration(), actual);
  }

  @Test
  void switchingPositionAlgorithmReinitializesOnlyPositionFromTransitionStart() {
    PerspectiveStateImpl intermediate = new PerspectiveStateImpl();
    transition.update(1_025.0, target, intermediate);

    transition.setPositionAlgorithm(FixedStartPositionTransitionAlgorithm.INSTANCE);
    PerspectiveStateImpl actual = new PerspectiveStateImpl();
    transition.update(1_050.0, target, actual);

    assertStateAtProgress(easedProgressAtHalfDuration(), actual);
  }

  @Test
  void rejectsNullAlgorithms() {
    assertThrows(NullPointerException.class, () -> transition.setPositionAlgorithm(null));
    assertThrows(NullPointerException.class, () -> transition.setRotationAlgorithm(null));
    assertThrows(NullPointerException.class, () -> transition.setFovAlgorithm(null));
    assertThrows(
        NullPointerException.class, () -> transition.setOrthographicHeightAlgorithm(null));
  }

  @Test
  void rejectsInvalidDuration() {
    assertThrows(IllegalArgumentException.class, () -> transition.setDurationMs(-1.0));
    assertThrows(IllegalArgumentException.class, () -> transition.setDurationMs(Double.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> transition.setDurationMs(Double.POSITIVE_INFINITY));
  }

  @Test
  void appliesCustomBlenderToEveryDefaultChannel() {
    transition.setBlender(progress -> progress * progress);
    PerspectiveStateImpl actual = new PerspectiveStateImpl();

    transition.update(1_050.0, target, actual);

    assertEquals(2.5, actual.position().x(), 1.0e-6);
    TestUtils.assertQuatEquals(rotation(22.5f), actual.rotation());
    assertEquals(70.0f, actual.getFovDeg(), 1.0e-4f);
    assertEquals(15.0f, actual.getOrthographicHeight(), 1.0e-4f);
  }

  @Test
  void appliesCustomBlenderToEveryAlternativeAlgorithm() {
    transition.setPositionAlgorithm(FixedStartPositionTransitionAlgorithm.INSTANCE);
    transition.setRotationAlgorithm(ChasingRotationTransitionAlgorithm.INSTANCE);
    transition.setBlender(progress -> progress * progress);
    PerspectiveStateImpl actual = new PerspectiveStateImpl();

    transition.update(1_050.0, target, actual);

    assertEquals(2.5, actual.position().x(), 1.0e-6);
    TestUtils.assertQuatEquals(rotation(22.5f), actual.rotation());
    assertEquals(70.0f, actual.getFovDeg(), 1.0e-4f);
    assertEquals(15.0f, actual.getOrthographicHeight(), 1.0e-4f);
  }

  @Test
  void rejectsNullBlender() {
    assertThrows(NullPointerException.class, () -> transition.setBlender(null));
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
  void inProgressUpdateSupportsAliasingTargetAndDestination() {
    transition.update(1_050.0, target, target);

    assertStateAtProgress(easedProgressAtHalfDuration(), target);
  }

  @Test
  void completedUpdateSupportsAliasingTargetAndDestination() {
    transition.update(1_100.0, target, target);

    PerspectiveStateImpl expected = state(10.0, 90.0f, 100.0f);
    expected.setOrthographicHeight(30.0f);
    assertStateEquals(expected, target);
  }

  private static void assertStateEquals(PerspectiveStateImpl expected, PerspectiveStateImpl actual) {
    TestUtils.assertVectorEquals(expected.position(), actual.position());
    TestUtils.assertQuatEquals(expected.rotation(), actual.rotation());
    assertEquals(expected.getFovDeg(), actual.getFovDeg());
    assertEquals(expected.projectionMode(), actual.projectionMode());
    assertEquals(expected.getOrthographicHeight(), actual.getOrthographicHeight());
  }

  private float easedProgressAtHalfDuration() {
    return transition.getBlender().blend(0.5f);
  }

  private static void assertStateAtProgress(float progress, PerspectiveStateImpl actual) {
    assertEquals(10.0f * progress, actual.position().x(), 1.0e-6);
    TestUtils.assertQuatEquals(rotation(90.0f * progress), actual.rotation());
    assertEquals(60.0f + 40.0f * progress, actual.getFovDeg(), 1.0e-4f);
    assertEquals(10.0f + 20.0f * progress, actual.getOrthographicHeight(), 1.0e-4f);
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
