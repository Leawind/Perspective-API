package io.github.leawind.perspectiveapi.internal.impl.transition;

import java.util.Objects;
import org.jspecify.annotations.NonNull;

/// Identifies the transition algorithms available for runtime comparison.
public enum TransitionAlgorithmType {
  FIXED_START_CHASING_ROTATION(
      "fixed_start_chasing_rotation", FixedStartChasingRotationTransitionAlgorithm::new),
  FEED_FORWARD_CORRECTION(
      "feed_forward_correction", FeedForwardCorrectionTransitionAlgorithm::new);

  private final String id;
  private final TransitionAlgorithmFactory factory;

  TransitionAlgorithmType(
      @NonNull String id, @NonNull TransitionAlgorithmFactory factory) {
    this.id = Objects.requireNonNull(id);
    this.factory = Objects.requireNonNull(factory);
  }

  public @NonNull String id() {
    return id;
  }

  public @NonNull TransitionAlgorithm create() {
    return Objects.requireNonNull(factory.create());
  }
}
