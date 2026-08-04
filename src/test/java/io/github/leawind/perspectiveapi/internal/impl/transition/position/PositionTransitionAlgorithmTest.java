package io.github.leawind.perspectiveapi.internal.impl.transition.position;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class PositionTransitionAlgorithmTest {
  @Test
  void fixedStartAlgorithmUsesTheProvidedProgress() {
    PositionTransitionAlgorithm algorithm = FixedStartPositionTransitionAlgorithm.INSTANCE;
    Vector3d dest = new Vector3d();
    algorithm.start(new Vector3d());

    algorithm.update(0.5f, new Vector3d(10.0, 0.0, 0.0), dest);

    assertEquals(5.0, dest.x(), 1.0e-6);
  }

  @Test
  void feedForwardCorrectionForwardsMovingTargetTranslation() {
    PositionTransitionAlgorithm algorithm = FeedForwardCorrectionPositionTransitionAlgorithm.INSTANCE;
    Vector3d target = new Vector3d(10.0, 0.0, 0.0);
    Vector3d dest = new Vector3d();
    algorithm.start(new Vector3d());
    algorithm.update(0.5f, target, dest);
    target.set(20.0, 0.0, 0.0);

    algorithm.update(0.84375f, target, dest);

    assertEquals(18.4375, dest.x(), 1.0e-6);
  }

  @Test
  void feedForwardCorrectionSupportsAliasingTargetAndDestination() {
    PositionTransitionAlgorithm algorithm = FeedForwardCorrectionPositionTransitionAlgorithm.INSTANCE;
    Vector3d targetAndDestination = new Vector3d(10.0, 0.0, 0.0);
    algorithm.start(new Vector3d());

    algorithm.update(0.5f, targetAndDestination, targetAndDestination);

    assertEquals(5.0, targetAndDestination.x(), 1.0e-6);
  }
}
