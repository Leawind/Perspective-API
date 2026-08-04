package io.github.leawind.perspectiveapi.internal.impl.transition;

import org.jspecify.annotations.NonNull;

/// Creates independent transition algorithm instances.
@FunctionalInterface
public interface TransitionAlgorithmFactory {

  @NonNull TransitionAlgorithm create();
}
