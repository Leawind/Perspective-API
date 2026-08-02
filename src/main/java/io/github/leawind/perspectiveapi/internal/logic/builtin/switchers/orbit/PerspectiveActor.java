package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics.BodyType;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics.PhysicsBody;
import java.util.Objects;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.jspecify.annotations.NonNull;

final class PerspectiveActor {
  private final @NonNull String perspectiveId;
  private final @NonNull PhysicsBody body;
  private final Vector2d grabOffset = new Vector2d();

  PerspectiveActor(@NonNull String perspectiveId) {
    this.perspectiveId = Objects.requireNonNull(perspectiveId);
    body = new PhysicsBody(perspectiveId, BodyType.DYNAMIC, 1);
    body.setCharge(1);
  }

  @NonNull String perspectiveId() {
    return perspectiveId;
  }

  @NonNull PhysicsBody body() {
    return body;
  }

  void beginDrag(Vector2dc mousePosition) {
    grabOffset.set(body.position()).sub(mousePosition);
  }

  void dragTo(Vector2dc mousePosition) {
    body.position().set(mousePosition).add(grabOffset);
  }
}
