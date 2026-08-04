package io.github.leawind.perspectiveapi.internal.impl.transition.rotation;

import io.github.leawind.perspectiveapi.testutils.TestUtils;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

class RotationTransitionAlgorithmTest {
  @Test
  void chasingRotationTracksMovingTarget() {
    RotationTransitionAlgorithm algorithm = ChasingRotationTransitionAlgorithm.INSTANCE;
    Quaternionf target = rotation(90.0f);
    Quaternionf dest = new Quaternionf();
    algorithm.start(new Quaternionf());
    algorithm.update(0.5f, target, dest);
    target.set(rotation(180.0f));

    algorithm.update(0.84375f, target, dest);

    TestUtils.assertQuatEquals(rotation(137.8125f), dest);
  }

  @Test
  void feedForwardCorrectionComposesTargetRotationInWorldSpace() {
    RotationTransitionAlgorithm algorithm =
        FeedForwardCorrectionRotationTransitionAlgorithm.INSTANCE;
    Quaternionf firstTarget = new Quaternionf().rotationXYZ(0.35f, -0.6f, 0.2f).normalize();
    Quaternionf worldDelta = new Quaternionf().rotationAxis(0.7f, 0.3f, 0.8f, -0.2f).normalize();
    Quaternionf stationaryDest = new Quaternionf();
    Quaternionf movingDest = new Quaternionf();
    algorithm.start(new Quaternionf());
    algorithm.update(0.352f, firstTarget, stationaryDest);
    algorithm.update(0.784f, firstTarget, stationaryDest);
    algorithm.start(new Quaternionf());
    algorithm.update(0.352f, firstTarget, movingDest);
    Quaternionf movingTarget = worldDelta.mul(firstTarget, new Quaternionf()).normalize();

    algorithm.update(0.784f, movingTarget, movingDest);

    Quaternionf expected = worldDelta.mul(stationaryDest, new Quaternionf()).normalize();
    TestUtils.assertQuatEquals(expected, movingDest);
  }

  @Test
  void feedForwardCorrectionIgnoresEquivalentQuaternionSignChanges() {
    RotationTransitionAlgorithm algorithm =
        FeedForwardCorrectionRotationTransitionAlgorithm.INSTANCE;
    Quaternionf target = rotation(90.0f);
    Quaternionf dest = new Quaternionf();
    algorithm.start(new Quaternionf());
    algorithm.update(0.5f, target, dest);
    target.mul(-1);

    algorithm.update(0.84375f, target, dest);

    TestUtils.assertQuatEquals(rotation(75.9375f), dest);
  }

  private static Quaternionf rotation(float yawDeg) {
    return new Quaternionf().rotationY((float) Math.toRadians(yawDeg));
  }
}
