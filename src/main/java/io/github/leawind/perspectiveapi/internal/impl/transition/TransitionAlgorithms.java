package io.github.leawind.perspectiveapi.internal.impl.transition;

import io.github.leawind.perspectiveapi.internal.impl.transition.position.FeedForwardCorrectionPositionTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.position.FixedStartPositionTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.position.PositionTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.rotation.ChasingRotationTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.rotation.FeedForwardCorrectionRotationTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.rotation.RotationTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.FixedStartFovTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.FixedStartOrthographicHeightTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.FovTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.OrthographicHeightTransitionAlgorithm;
import java.util.List;

/// Built-in transition algorithms available in the development configuration screen.
public final class TransitionAlgorithms {
  public static final List<PositionTransitionAlgorithm> POSITION =
      List.of(
          FixedStartPositionTransitionAlgorithm.INSTANCE,
          FeedForwardCorrectionPositionTransitionAlgorithm.INSTANCE);
  public static final List<RotationTransitionAlgorithm> ROTATION =
      List.of(
          ChasingRotationTransitionAlgorithm.INSTANCE,
          FeedForwardCorrectionRotationTransitionAlgorithm.INSTANCE);
  public static final List<FovTransitionAlgorithm> FOV =
      List.of(FixedStartFovTransitionAlgorithm.INSTANCE);
  public static final List<OrthographicHeightTransitionAlgorithm> ORTHOGRAPHIC_HEIGHT =
      List.of(FixedStartOrthographicHeightTransitionAlgorithm.INSTANCE);

  private TransitionAlgorithms() {}
}
