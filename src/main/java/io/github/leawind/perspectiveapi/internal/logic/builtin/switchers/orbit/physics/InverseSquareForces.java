package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics;

public final class InverseSquareForces {
  private InverseSquareForces() {}

  public static PairForce gravity(double factor, double softening, double maxForce) {
    validate(factor, softening, maxForce);
    return (first, second) ->
        apply(first, second, factor * first.mass() * second.mass(), softening, maxForce);
  }

  public static PairForce coulomb(double factor, double softening, double maxForce) {
    validate(factor, softening, maxForce);
    return (first, second) -> {
      double product = first.charge() * second.charge();
      if (product == 0) return;
      apply(first, second, -factor * product, softening, maxForce);
    };
  }

  private static void apply(
      PhysicsBody first, PhysicsBody second, double strength, double softening, double maxForce) {
    double dx = second.position().x - first.position().x;
    double dy = second.position().y - first.position().y;
    double distanceSquared = dx * dx + dy * dy;
    if (distanceSquared < 1e-20) return;

    double softenedSquared = distanceSquared + softening * softening;
    double scale = strength / (softenedSquared * Math.sqrt(distanceSquared));
    double fx = dx * scale;
    double fy = dy * scale;
    double forceSquared = fx * fx + fy * fy;
    if (forceSquared > maxForce * maxForce) {
      double clamp = maxForce / Math.sqrt(forceSquared);
      fx *= clamp;
      fy *= clamp;
    }

    if (first.type() == BodyType.DYNAMIC) first.addForce(fx, fy);
    if (second.type() == BodyType.DYNAMIC) second.addForce(-fx, -fy);
  }

  private static void validate(double factor, double softening, double maxForce) {
    if (!Double.isFinite(factor)) throw new IllegalArgumentException("factor must be finite");
    if (!Double.isFinite(softening) || softening <= 0) {
      throw new IllegalArgumentException("softening must be finite and positive");
    }
    if (!Double.isFinite(maxForce) || maxForce <= 0) {
      throw new IllegalArgumentException("maxForce must be finite and positive");
    }
  }
}
