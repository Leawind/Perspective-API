package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics;

import org.jspecify.annotations.NonNull;

public final class DragForce implements BodyForce {
  private final double linearFactor;
  private final double quadraticFactor;

  public DragForce(double linearFactor, double quadraticFactor) {
    if (!Double.isFinite(linearFactor) || linearFactor < 0) {
      throw new IllegalArgumentException("linearFactor must be finite and non-negative");
    }
    if (!Double.isFinite(quadraticFactor) || quadraticFactor < 0) {
      throw new IllegalArgumentException("quadraticFactor must be finite and non-negative");
    }
    this.linearFactor = linearFactor;
    this.quadraticFactor = quadraticFactor;
  }

  @Override
  public void apply(@NonNull PhysicsBody body) {
    if (body.type() != BodyType.DYNAMIC) return;
    double speed = body.velocity().length();
    double factor = linearFactor + quadraticFactor * speed;
    body.addForce(-factor * body.velocity().x, -factor * body.velocity().y);
  }
}
