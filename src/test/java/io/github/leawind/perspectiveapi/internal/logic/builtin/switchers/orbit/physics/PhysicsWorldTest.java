package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PhysicsWorldTest {
  @Test
  void staticBodyCanAttractDynamicBody() {
    PhysicsWorld world = new PhysicsWorld();
    PhysicsBody anchor = new PhysicsBody("anchor", BodyType.STATIC, 10);
    PhysicsBody actor = new PhysicsBody("actor", BodyType.DYNAMIC, 1);
    actor.position().set(1, 0);
    world.addBody(anchor);
    world.addBody(actor);
    world.addPairForce(InverseSquareForces.gravity(1, 0.01, 100));

    world.advance(PhysicsWorld.DEFAULT_STEP_SECONDS);

    assertTrue(actor.velocity().x < 0);
    assertEquals(0, anchor.velocity().lengthSquared());
  }

  @Test
  void kinematicBodyDoesNotMoveButCanRepel() {
    PhysicsWorld world = new PhysicsWorld();
    PhysicsBody dragged = new PhysicsBody("dragged", BodyType.KINEMATIC, 1);
    dragged.setCharge(1);
    PhysicsBody actor = new PhysicsBody("actor", BodyType.DYNAMIC, 1);
    actor.setCharge(1);
    actor.position().set(0.01, 0);
    world.addBody(dragged);
    world.addBody(actor);
    world.addPairForce(InverseSquareForces.coulomb(100, 0.05, 5));

    world.advance(PhysicsWorld.DEFAULT_STEP_SECONDS);

    assertEquals(0, dragged.position().lengthSquared());
    assertTrue(actor.velocity().x > 0);
  }

  @Test
  void closeBodiesProduceFiniteClampedForce() {
    PhysicsBody first = new PhysicsBody("first", BodyType.DYNAMIC, 1);
    PhysicsBody second = new PhysicsBody("second", BodyType.DYNAMIC, 1);
    first.setCharge(1);
    second.setCharge(1);
    second.position().set(1e-8, 0);

    PairForce force = InverseSquareForces.coulomb(1e9, 0.01, 3);
    first.prepareStep();
    second.prepareStep();
    force.apply(first, second);

    assertTrue(first.totalForce().isFinite());
    assertTrue(second.totalForce().isFinite());
    assertTrue(first.totalForce().length() <= 3.000001);
  }

  @Test
  void bodyIdentityNeverChanges() {
    Object identity = new Object();
    PhysicsBody body = new PhysicsBody(identity, BodyType.DYNAMIC, 1);
    body.setType(BodyType.KINEMATIC);
    body.setType(BodyType.DYNAMIC);

    assertSame(identity, body.identity());
  }
}
