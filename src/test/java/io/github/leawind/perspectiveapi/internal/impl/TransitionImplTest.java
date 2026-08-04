package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.ProjectionMode;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blender;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blenders;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import org.joml.Quaternionf;
import org.joml.Vector3d;
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
    transition.setBlendPower(1.0);
    transition.setBlender(Blenders::linear);
    start = state(0.0, 0.0f, 60.0f);
    target = state(10.0, 90.0f, 100.0f);
    transition.setStartState(1_000.0, start);
  }

  @Test
  void defaultSettingsAndSettersAreExposed() {
    TransitionImpl defaults = new TransitionImpl();
    assertEquals(260.0, defaults.getDurationMs());
    assertEquals(0.6, defaults.getBlendPower());
    Blender blender = x -> x * x;
    defaults.setDurationMs(500.0);
    defaults.setBlendPower(2.0);
    defaults.setBlender(blender);

    assertEquals(500.0, defaults.getDurationMs());
    assertEquals(2.0, defaults.getBlendPower());
    assertSame(blender, defaults.getBlender());
  }

  @Test
  void rejectsInvalidSettings() {
    assertThrows(IllegalArgumentException.class, () -> transition.setDurationMs(-1.0));
    assertThrows(IllegalArgumentException.class, () -> transition.setDurationMs(Double.NaN));
    assertThrows(
        IllegalArgumentException.class, () -> transition.setDurationMs(Double.POSITIVE_INFINITY));
    assertThrows(IllegalArgumentException.class, () -> transition.setBlendPower(0.0));
    assertThrows(IllegalArgumentException.class, () -> transition.setBlendPower(-1.0));
    assertThrows(IllegalArgumentException.class, () -> transition.setBlendPower(Double.NaN));
    assertThrows(NullPointerException.class, () -> transition.setBlender(null));
  }

  @Test
  void transitionBoundaryUsesDuration() {
    assertTrue(transition.isInTransition(1_000.0));
    assertTrue(transition.isInTransition(1_099.999));
    assertFalse(transition.isInTransition(1_100.0));
  }

  @Test
  void interpolatesAllFieldsAtHalfwayPoint() {
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    transition.update(1_050.0, target, dest);

    assertHalfway(dest);
  }

  @Test
  void updateClampsToTargetAfterTransition() {
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    transition.update(1_200.0, target, dest);

    TestUtils.assertVectorEquals(target.position(), dest.position());
    TestUtils.assertQuatEquals(target.rotation(), dest.rotation());
    assertEquals(target.getFovDeg(), dest.getFovDeg());
  }

  @Test
  void updateSupportsAliasingTargetAndDestination() {
    transition.update(1_050.0, target, target);

    assertHalfway(target);
  }

  @Test
  void zeroDurationCompletesImmediately() {
    transition.setDurationMs(0.0);
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    transition.update(1_000.0, target, dest);

    TestUtils.assertVectorEquals(target.position(), dest.position());
    TestUtils.assertQuatEquals(target.rotation(), dest.rotation());
    assertEquals(target.getFovDeg(), dest.getFovDeg());
  }

  @Test
  void interpolatesOrthographicHeightWhenProjectionModeMatches() {
    start.setProjectionMode(ProjectionMode.ORTHOGRAPHIC);
    start.setOrthographicHeight(10.0f);
    target.setProjectionMode(ProjectionMode.ORTHOGRAPHIC);
    target.setOrthographicHeight(30.0f);
    transition.setStartState(1_000.0, start);
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    transition.update(1_050.0, target, dest);

    assertEquals(ProjectionMode.ORTHOGRAPHIC, dest.projectionMode());
    assertEquals(20.0f, dest.getOrthographicHeight(), 1.0e-4f);
  }

  @Test
  void switchesProjectionModeDiscretelyAndInterpolatesOrthographicHeight() {
    start.setProjectionMode(ProjectionMode.PERSPECTIVE);
    start.setOrthographicHeight(10.0f);
    target.setProjectionMode(ProjectionMode.ORTHOGRAPHIC);
    target.setOrthographicHeight(30.0f);
    transition.setStartState(1_000.0, start);
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    transition.update(1_001.0, target, dest);

    assertEquals(ProjectionMode.ORTHOGRAPHIC, dest.projectionMode());
    assertEquals(10.2f, dest.getOrthographicHeight(), 1.0e-4f);
  }

  private void assertHalfway(PerspectiveStateImpl actual) {
    TestUtils.assertVectorEquals(new Vector3d(5.0, 0.0, 0.0), actual.position());
    TestUtils.assertQuatEquals(
        new Quaternionf().rotationY((float) Math.toRadians(45.0)), actual.rotation());
    assertEquals(80.0f, actual.getFovDeg(), 1.0e-4f);
  }

  private static PerspectiveStateImpl state(double x, float yawDeg, float fovDeg) {
    PerspectiveStateImpl state = new PerspectiveStateImpl();
    state.position().set(x, 0.0, 0.0);
    state.rotation().rotationY((float) Math.toRadians(yawDeg));
    state.setFovDeg(fovDeg);
    return state;
  }
}
