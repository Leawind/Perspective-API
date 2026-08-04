package io.github.leawind.perspectiveapi.internal.impl.transition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveStateImpl;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blender;
import io.github.leawind.perspectiveapi.internal.utils.smooth.Blenders;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixedStartChasingRotationTransitionAlgorithmTest {
  private FixedStartChasingRotationTransitionAlgorithm algorithm;
  private PerspectiveStateImpl start;
  private PerspectiveStateImpl target;

  @BeforeEach
  void beforeEach() {
    algorithm = new FixedStartChasingRotationTransitionAlgorithm();
    algorithm.setBlendPower(1.0);
    algorithm.setBlender(Blenders::linear);
    start = state(0.0, 0.0f, 60.0f);
    target = state(10.0, 90.0f, 100.0f);
    algorithm.start(start);
  }

  @Test
  void exposesAlgorithmSpecificDefaultsAndSetters() {
    FixedStartChasingRotationTransitionAlgorithm defaults =
        new FixedStartChasingRotationTransitionAlgorithm();
    Blender blender = x -> x * x;

    defaults.setBlendPower(2.0);
    defaults.setBlender(blender);

    assertEquals(2.0, defaults.getBlendPower());
    assertSame(blender, defaults.getBlender());
  }

  @Test
  void rejectsInvalidAlgorithmSettings() {
    assertThrows(IllegalArgumentException.class, () -> algorithm.setBlendPower(0.0));
    assertThrows(IllegalArgumentException.class, () -> algorithm.setBlendPower(-1.0));
    assertThrows(IllegalArgumentException.class, () -> algorithm.setBlendPower(Double.NaN));
    assertThrows(NullPointerException.class, () -> algorithm.setBlender(null));
  }

  @Test
  void interpolatesContinuousFieldsAtHalfwayPoint() {
    start.setOrthographicHeight(10.0f);
    target.setOrthographicHeight(30.0f);
    algorithm.start(start);
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    algorithm.update(50.0, 100.0, target, dest);

    TestUtils.assertVectorEquals(new Vector3d(5.0, 0.0, 0.0), dest.position());
    TestUtils.assertQuatEquals(rotation(45.0f), dest.rotation());
    assertEquals(80.0f, dest.getFovDeg(), 1.0e-4f);
    assertEquals(20.0f, dest.getOrthographicHeight(), 1.0e-4f);
  }

  @Test
  void updateSupportsAliasingTargetAndDestination() {
    algorithm.update(50.0, 100.0, target, target);

    TestUtils.assertVectorEquals(new Vector3d(5.0, 0.0, 0.0), target.position());
    TestUtils.assertQuatEquals(rotation(45.0f), target.rotation());
    assertEquals(80.0f, target.getFovDeg(), 1.0e-4f);
  }

  @Test
  void chasesMovingRotationTarget() {
    PerspectiveStateImpl dest = new PerspectiveStateImpl();
    algorithm.update(50.0, 100.0, target, dest);
    target.rotation().set(rotation(180.0f));

    algorithm.update(75.0, 100.0, target, dest);

    TestUtils.assertQuatEquals(rotation(112.5f), dest.rotation());
  }

  @Test
  void startCopiesValuesAndResetsIntermediateState() {
    PerspectiveStateImpl dest = new PerspectiveStateImpl();
    algorithm.update(50.0, 100.0, target, dest);
    PerspectiveStateImpl replacementStart = state(20.0, 180.0f, 120.0f);
    algorithm.start(replacementStart);
    replacementStart.position().zero();
    replacementStart.rotation().identity();
    replacementStart.setFovDeg(70.0f);

    algorithm.update(50.0, 100.0, state(30.0, 90.0f, 100.0f), dest);

    TestUtils.assertVectorEquals(new Vector3d(25.0, 0.0, 0.0), dest.position());
    TestUtils.assertQuatEquals(rotation(135.0f), dest.rotation());
    assertEquals(110.0f, dest.getFovDeg(), 1.0e-4f);
  }

  @Test
  void factoryCreatesIndependentInstances() {
    TransitionAlgorithmFactory factory = FixedStartChasingRotationTransitionAlgorithm::new;

    TransitionAlgorithm first = factory.create();
    TransitionAlgorithm second = factory.create();

    assertNotSame(first, second);
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
