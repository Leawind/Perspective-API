package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.ProjectionMode;
import java.util.Objects;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

public class PerspectiveStateImpl implements PerspectiveState.Mutable {
  public static final float DEFAULT_FOV_DEGREES = 70;
  public static final float DEFAULT_ORTHOGRAPHIC_HEIGHT = 16;

  private final Vector3d position = new Vector3d();
  private final Quaternionf rotation = new Quaternionf();
  private float fovDeg = DEFAULT_FOV_DEGREES;
  private ProjectionMode projectionMode = ProjectionMode.PERSPECTIVE;
  private float orthographicHeight = DEFAULT_ORTHOGRAPHIC_HEIGHT;

  @Override
  public @NonNull Vector3d position() {
    return position;
  }

  @Override
  public @NonNull Quaternionf rotation() {
    return rotation;
  }

  @Override
  public @NonNull ProjectionMode getProjectionMode() {
    return projectionMode;
  }

  @Override
  public void setFovDeg(float fovDeg) {
    this.fovDeg = fovDeg;
  }

  @Override
  public float getFovDeg() {
    return fovDeg;
  }

  @Override
  public void setProjectionMode(@NonNull ProjectionMode projectionMode) {
    this.projectionMode = Objects.requireNonNull(projectionMode);
  }

  @Override
  public float getOrthographicHeight() {
    return orthographicHeight;
  }

  @Override
  public void setOrthographicHeight(float orthographicHeight) {
    this.orthographicHeight = orthographicHeight;
  }

  /// Copies all fields from the given source state.
  public PerspectiveStateImpl set(@NonNull PerspectiveState source) {
    position.set(source.position());
    rotation.set(source.rotation());
    fovDeg = source.getFovDeg();
    projectionMode = Objects.requireNonNull(source.getProjectionMode());
    orthographicHeight = source.getOrthographicHeight();
    return this;
  }
}
