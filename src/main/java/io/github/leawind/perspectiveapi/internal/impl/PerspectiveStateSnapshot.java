package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.ProjectionMode;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

/// An independent read-only copy of a camera state.
public final class PerspectiveStateSnapshot implements PerspectiveState {
  private final Vector3d position;
  private final Quaternionf rotation;
  private final ProjectionMode projectionMode;
  private final float fovDeg;
  private final float orthographicHeight;

  public PerspectiveStateSnapshot(@NonNull PerspectiveState source) {
    Objects.requireNonNull(source);
    position = new Vector3d(source.position());
    rotation = new Quaternionf(source.rotation());
    projectionMode = source.projectionMode();
    fovDeg = source.getFovDeg();
    orthographicHeight = source.getOrthographicHeight();
  }

  @Override
  public @NonNull Vector3dc position() {
    return position;
  }

  @Override
  public @NonNull Quaternionfc rotation() {
    return rotation;
  }

  @Override
  public @NonNull ProjectionMode projectionMode() {
    return projectionMode;
  }

  @Override
  public float getFovDeg() {
    return fovDeg;
  }

  @Override
  public float getOrthographicHeight() {
    return orthographicHeight;
  }
}
