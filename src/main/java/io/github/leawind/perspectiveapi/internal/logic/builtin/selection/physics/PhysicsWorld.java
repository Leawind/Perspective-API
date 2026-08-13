package io.github.leawind.perspectiveapi.internal.logic.builtin.selection.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

public final class PhysicsWorld {
  public static final double DEFAULT_STEP_SECONDS = 1.0 / 120.0;

  private final List<PhysicsBody> bodies = new ArrayList<>();
  private final List<BodyForce> bodyForces = new ArrayList<>();
  private final List<PairForce> pairForces = new ArrayList<>();

  private double stepSeconds = DEFAULT_STEP_SECONDS;
  private double maxFrameSeconds = 0.1;
  private int maxSubSteps = 8;
  private double maxTotalForce = 200.0;
  private double maxSpeed = 4.0;
  private double accumulator;

  public @NonNull List<@NonNull PhysicsBody> bodies() {
    return Collections.unmodifiableList(bodies);
  }

  public void addBody(@NonNull PhysicsBody body) {
    Objects.requireNonNull(body);
    if (!bodies.contains(body)) bodies.add(body);
  }

  public void removeBody(@NonNull PhysicsBody body) {
    bodies.remove(Objects.requireNonNull(body));
  }

  public void addBodyForce(@NonNull BodyForce force) {
    bodyForces.add(Objects.requireNonNull(force));
  }

  public void addPairForce(@NonNull PairForce force) {
    pairForces.add(Objects.requireNonNull(force));
  }

  public void setMaxTotalForce(double maxTotalForce) {
    requirePositiveFinite(maxTotalForce, "maxTotalForce");
    this.maxTotalForce = maxTotalForce;
  }

  public void setMaxSpeed(double maxSpeed) {
    requirePositiveFinite(maxSpeed, "maxSpeed");
    this.maxSpeed = maxSpeed;
  }

  public void advance(double frameSeconds) {
    if (!Double.isFinite(frameSeconds) || frameSeconds <= 0) return;
    accumulator += Math.min(frameSeconds, maxFrameSeconds);

    int steps = 0;
    while (accumulator >= stepSeconds && steps++ < maxSubSteps) {
      step(stepSeconds);
      accumulator -= stepSeconds;
    }
    if (steps > maxSubSteps) accumulator = 0;
  }

  public void resetClock() {
    accumulator = 0;
  }

  void step(double deltaSeconds) {
    for (PhysicsBody body : bodies) body.prepareStep();

    for (PhysicsBody body : bodies) {
      for (BodyForce force : bodyForces) force.apply(body);
    }

    for (int i = 0; i < bodies.size(); i++) {
      PhysicsBody first = bodies.get(i);
      for (int j = i + 1; j < bodies.size(); j++) {
        PhysicsBody second = bodies.get(j);
        for (PairForce force : pairForces) force.apply(first, second);
      }
    }

    for (PhysicsBody body : bodies) {
      body.clampForce(maxTotalForce);
      body.integrate(deltaSeconds, maxSpeed);
    }
  }

  private static void requirePositiveFinite(double value, String name) {
    if (!Double.isFinite(value) || value <= 0) {
      throw new IllegalArgumentException(name + " must be finite and positive");
    }
  }
}
