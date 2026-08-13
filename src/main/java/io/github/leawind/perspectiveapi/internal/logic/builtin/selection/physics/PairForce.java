package io.github.leawind.perspectiveapi.internal.logic.builtin.selection.physics;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface PairForce {
  void apply(@NonNull PhysicsBody first, @NonNull PhysicsBody second);
}
