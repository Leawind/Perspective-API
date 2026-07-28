package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics;

import java.util.Objects;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.jspecify.annotations.NonNull;

public final class PhysicsBody {
  private final @NonNull Object identity;
  private @NonNull BodyType type;
  private double mass;
  private double charge;

  private final Vector2d position = new Vector2d();
  private final Vector2d velocity = new Vector2d();
  private final Vector2d totalForce = new Vector2d();
  private final Vector2d lastFinitePosition = new Vector2d();

  public PhysicsBody(@NonNull Object identity, @NonNull BodyType type, double mass) {
    this.identity = Objects.requireNonNull(identity);
    this.type = Objects.requireNonNull(type);
    setMass(mass);
  }

  public @NonNull Object identity() {
    return identity;
  }

  public @NonNull BodyType type() {
    return type;
  }

  public void setType(@NonNull BodyType type) {
    this.type = Objects.requireNonNull(type);
    if (type == BodyType.STATIC) velocity.zero();
  }

  public double mass() {
    return mass;
  }

  public void setMass(double mass) {
    if (!Double.isFinite(mass) || mass <= 0) {
      throw new IllegalArgumentException("mass must be finite and positive");
    }
    this.mass = mass;
  }

  public double charge() {
    return charge;
  }

  public void setCharge(double charge) {
    if (!Double.isFinite(charge)) throw new IllegalArgumentException("charge must be finite");
    this.charge = charge;
  }

  public @NonNull Vector2d position() {
    return position;
  }

  public @NonNull Vector2d velocity() {
    return velocity;
  }

  public @NonNull Vector2dc totalForce() {
    return totalForce;
  }

  public void addForce(double x, double y) {
    if (!Double.isFinite(x) || !Double.isFinite(y)) return;
    totalForce.add(x, y);
  }

  public void clearForce() {
    totalForce.zero();
  }

  void prepareStep() {
    totalForce.zero();
    if (position.isFinite()) lastFinitePosition.set(position);
  }

  void clampForce(double maxForce) {
    clampLength(totalForce, maxForce);
  }

  void integrate(double deltaSeconds, double maxSpeed) {
    if (type != BodyType.DYNAMIC) return;

    velocity.fma(deltaSeconds / mass, totalForce);
    clampLength(velocity, maxSpeed);
    position.fma(deltaSeconds, velocity);

    if (!position.isFinite() || !velocity.isFinite()) {
      position.set(lastFinitePosition);
      velocity.zero();
    }
  }

  private static void clampLength(Vector2d vector, double maxLength) {
    double lengthSquared = vector.lengthSquared();
    double maxLengthSquared = maxLength * maxLength;
    if (lengthSquared > maxLengthSquared) {
      vector.mul(maxLength / Math.sqrt(lengthSquared));
    }
  }
}
