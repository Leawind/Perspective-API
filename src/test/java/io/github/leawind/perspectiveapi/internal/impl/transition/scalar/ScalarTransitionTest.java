package io.github.leawind.perspectiveapi.internal.impl.transition.scalar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ScalarTransitionTest {

  @Test
  void fixedStartAlgorithmInterpolatesFromItsOwnStartValue() {
    FovTransitionAlgorithm algorithm = FixedStartFovTransitionAlgorithm.INSTANCE;
    algorithm.start(60.0f);

    float result = algorithm.update(0.5f, 100.0f);

    assertEquals(80.0f, result, 1.0e-4f);
  }

  @Test
  void fovAndOrthographicHeightKeepIndependentState() {
    FovTransitionAlgorithm fov = FixedStartFovTransitionAlgorithm.INSTANCE;
    OrthographicHeightTransitionAlgorithm height =
        FixedStartOrthographicHeightTransitionAlgorithm.INSTANCE;
    fov.start(60.0f);
    height.start(10.0f);

    assertEquals(80.0f, fov.update(0.5f, 100.0f), 1.0e-4f);
    assertEquals(20.0f, height.update(0.5f, 30.0f), 1.0e-4f);
  }
}
