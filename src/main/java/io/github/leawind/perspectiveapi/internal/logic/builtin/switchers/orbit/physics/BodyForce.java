package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics;

import org.jspecify.annotations.NonNull;

@FunctionalInterface
public interface BodyForce {
  void apply(@NonNull PhysicsBody body);
}
