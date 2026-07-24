package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

public class PerspectiveStateImpl implements PerspectiveState.Mutable {
  public static final float DEFAULT_FOV_DEGREES = 70;

  private final Vector3d position = new Vector3d();
  private final Quaternionf rotation = new Quaternionf();
  private float fovDeg = DEFAULT_FOV_DEGREES;

  @Override
  public @NonNull Vector3d position() {
    return position;
  }

  @Override
  public @NonNull Quaternionf rotation() {
    return rotation;
  }

  @Override
  public void setFovDeg(float fovDeg) {
    this.fovDeg = fovDeg;
  }

  @Override
  public float getFovDeg() {
    return fovDeg;
  }

  /// Copies all fields from the given source state.
  public PerspectiveStateImpl set(@NonNull PerspectiveState source) {
    position.set(source.position());
    rotation.set(source.rotation());
    fovDeg = source.getFovDeg();
    return this;
  }
}
