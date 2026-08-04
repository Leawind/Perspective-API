package io.github.leawind.perspectiveapi.internal.impl.transition;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class FeedForwardCorrectionTransitionAlgorithmTest {
  private static final double DURATION_MS = 100.0;

  private FeedForwardCorrectionTransitionAlgorithm algorithm;
  private PerspectiveStateImpl start;
  private PerspectiveStateImpl target;

  @BeforeEach
  void beforeEach() {
    algorithm = new FeedForwardCorrectionTransitionAlgorithm();
    algorithm.setBlender(Blenders::linear);
    start = state(0.0, 0.0f, 60.0f);
    target = state(10.0, 90.0f, 100.0f);
    algorithm.start(start);
  }

  @Test
  void exposesAlgorithmSpecificBlender() {
    FeedForwardCorrectionTransitionAlgorithm defaults =
        new FeedForwardCorrectionTransitionAlgorithm();
    assertEquals(0.15625f, defaults.getBlender().blend(0.25f), 1.0e-6f);
    Blender blender = x -> x * x;

    defaults.setBlender(blender);

    assertSame(blender, defaults.getBlender());
    assertThrows(NullPointerException.class, () -> defaults.setBlender(null));
  }

  @Test
  void appliesEasingToCorrectionAndProjectionFields() {
    algorithm.setBlender(x -> x * x);
    start.setOrthographicHeight(10.0f);
    target.setOrthographicHeight(30.0f);
    algorithm.start(start);
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    algorithm.update(25.0, DURATION_MS, target, dest);
    algorithm.update(50.0, DURATION_MS, target, dest);

    TestUtils.assertVectorEquals(new Vector3d(2.5, 0.0, 0.0), dest.position());
    TestUtils.assertQuatEquals(rotation(22.5f), dest.rotation());
    assertEquals(70.0f, dest.getFovDeg(), 1.0e-4f);
    assertEquals(15.0f, dest.getOrthographicHeight(), 1.0e-4f);
  }

  @Test
  void correctsStaticTargetAccordingToRemainingTime() {
    start.setOrthographicHeight(10.0f);
    target.setOrthographicHeight(30.0f);
    algorithm.start(start);
    PerspectiveStateImpl dest = new PerspectiveStateImpl();

    algorithm.update(20.0, DURATION_MS, target, dest);
    algorithm.update(50.0, DURATION_MS, target, dest);

    TestUtils.assertVectorEquals(new Vector3d(5.0, 0.0, 0.0), dest.position());
    TestUtils.assertQuatEquals(rotation(45.0f), dest.rotation());
    assertEquals(80.0f, dest.getFovDeg(), 1.0e-4f);
    assertEquals(20.0f, dest.getOrthographicHeight(), 1.0e-4f);
  }

  @Test
  void forwardsTargetTranslationAndWorldRotationWithoutAttenuation() {
    PerspectiveStateImpl dest = new PerspectiveStateImpl();
    algorithm.update(50.0, DURATION_MS, target, dest);
    target.position().set(20.0, 0.0, 0.0);
    target.rotation().set(rotation(180.0f));

    algorithm.update(75.0, DURATION_MS, target, dest);

    TestUtils.assertVectorEquals(new Vector3d(17.5, 0.0, 0.0), dest.position());
    TestUtils.assertQuatEquals(rotation(157.5f), dest.rotation());
  }

  @Test
  void easingDoesNotAttenuateFeedForwardMotion() {
    FeedForwardCorrectionTransitionAlgorithm stationaryAlgorithm =
        new FeedForwardCorrectionTransitionAlgorithm();
    FeedForwardCorrectionTransitionAlgorithm movingAlgorithm =
        new FeedForwardCorrectionTransitionAlgorithm();
    Blender quadratic = x -> x * x;
    stationaryAlgorithm.setBlender(quadratic);
    movingAlgorithm.setBlender(quadratic);
    stationaryAlgorithm.start(start);
    movingAlgorithm.start(start);
    PerspectiveStateImpl stationaryTarget = state(10.0, 90.0f, 100.0f);
    PerspectiveStateImpl movingTarget = state(10.0, 90.0f, 100.0f);
    PerspectiveStateImpl stationaryDest = new PerspectiveStateImpl();
    PerspectiveStateImpl movingDest = new PerspectiveStateImpl();
    stationaryAlgorithm.update(50.0, DURATION_MS, stationaryTarget, stationaryDest);
    movingAlgorithm.update(50.0, DURATION_MS, movingTarget, movingDest);
    movingTarget.position().add(10.0, 0.0, 0.0);
    movingTarget.rotation().set(rotation(180.0f));

    stationaryAlgorithm.update(75.0, DURATION_MS, stationaryTarget, stationaryDest);
    movingAlgorithm.update(75.0, DURATION_MS, movingTarget, movingDest);

    assertEquals(stationaryDest.position().x() + 10.0, movingDest.position().x(), 1.0e-6);
    Quaternionf expected = rotation(90.0f).mul(stationaryDest.rotation(), new Quaternionf());
    TestUtils.assertQuatEquals(expected, movingDest.rotation());
  }

  @Test
  void composesFeedForwardRotationInWorldSpace() {
    FeedForwardCorrectionTransitionAlgorithm stationaryAlgorithm =
        new FeedForwardCorrectionTransitionAlgorithm();
    FeedForwardCorrectionTransitionAlgorithm movingAlgorithm =
        new FeedForwardCorrectionTransitionAlgorithm();
    PerspectiveStateImpl sharedStart = state(0.0, 0.0f, 70.0f);
    Quaternionf firstTargetRotation =
        new Quaternionf().rotationXYZ(0.35f, -0.6f, 0.2f).normalize();
    Quaternionf worldDelta = new Quaternionf().rotationAxis(0.7f, 0.3f, 0.8f, -0.2f).normalize();
    PerspectiveStateImpl stationaryTarget = state(0.0, 0.0f, 70.0f);
    stationaryTarget.rotation().set(firstTargetRotation);
    PerspectiveStateImpl movingTarget = state(0.0, 0.0f, 70.0f);
    movingTarget.rotation().set(firstTargetRotation);
    PerspectiveStateImpl stationaryDest = new PerspectiveStateImpl();
    PerspectiveStateImpl movingDest = new PerspectiveStateImpl();
    stationaryAlgorithm.start(sharedStart);
    movingAlgorithm.start(sharedStart);
    stationaryAlgorithm.update(40.0, DURATION_MS, stationaryTarget, stationaryDest);
    movingAlgorithm.update(40.0, DURATION_MS, movingTarget, movingDest);
    movingTarget.rotation().set(worldDelta.mul(firstTargetRotation, new Quaternionf())).normalize();

    stationaryAlgorithm.update(70.0, DURATION_MS, stationaryTarget, stationaryDest);
    movingAlgorithm.update(70.0, DURATION_MS, movingTarget, movingDest);

    Quaternionf expected = worldDelta.mul(stationaryDest.rotation(), new Quaternionf()).normalize();
    TestUtils.assertQuatEquals(expected, movingDest.rotation());
  }

  @Test
  void ignoresEquivalentTargetQuaternionSignChanges() {
    PerspectiveStateImpl dest = new PerspectiveStateImpl();
    algorithm.update(50.0, DURATION_MS, target, dest);
    target.rotation().mul(-1);

    algorithm.update(75.0, DURATION_MS, target, dest);

    TestUtils.assertQuatEquals(rotation(67.5f), dest.rotation());
  }

  @Test
  void updateSupportsAliasingTargetAndDestination() {
    algorithm.update(50.0, DURATION_MS, target, target);

    TestUtils.assertVectorEquals(new Vector3d(5.0, 0.0, 0.0), target.position());
    TestUtils.assertQuatEquals(rotation(45.0f), target.rotation());
    assertEquals(80.0f, target.getFovDeg(), 1.0e-4f);
  }

  @Test
  void startCopiesValuesAndResetsPreviousTargetSample() {
    PerspectiveStateImpl dest = new PerspectiveStateImpl();
    algorithm.update(50.0, DURATION_MS, target, dest);
    PerspectiveStateImpl replacementStart = state(20.0, 180.0f, 120.0f);
    algorithm.start(replacementStart);
    replacementStart.position().zero();
    replacementStart.rotation().identity();

    algorithm.update(50.0, DURATION_MS, state(30.0, 90.0f, 100.0f), dest);

    TestUtils.assertVectorEquals(new Vector3d(25.0, 0.0, 0.0), dest.position());
    TestUtils.assertQuatEquals(rotation(135.0f), dest.rotation());
    assertEquals(110.0f, dest.getFovDeg(), 1.0e-4f);
  }

  @Test
  void repeatedTimestampStillForwardsNewTargetMotion() {
    PerspectiveStateImpl dest = new PerspectiveStateImpl();
    algorithm.update(50.0, DURATION_MS, target, dest);
    target.position().add(4.0, 0.0, 0.0);
    target.rotation().rotateY((float) Math.toRadians(30.0));

    algorithm.update(50.0, DURATION_MS, target, dest);

    TestUtils.assertVectorEquals(new Vector3d(9.0, 0.0, 0.0), dest.position());
    TestUtils.assertQuatEquals(rotation(75.0f), dest.rotation());
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
