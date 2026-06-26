package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

public class PerspectiveStateImpl implements PerspectiveState.Mutable {
  private final Vector3d position = new Vector3d();
  private final Quaternionf rotation = new Quaternionf();
  private boolean hasFov;
  private float fieldOfView;
  private float fieldOfViewModifier = 1.0f;

  public PerspectiveStateImpl() {}

  @Override
  public @NonNull Vector3d position() {
    return position;
  }

  @Override
  public @NonNull Quaternionf rotation() {
    return rotation;
  }

  @Override
  public boolean hasFieldOfView() {
    return hasFov;
  }

  @Override
  public float getFieldOfView() {
    return fieldOfView;
  }

  @Override
  public float getFieldOfViewModifier() {
    return fieldOfViewModifier;
  }

  @Override
  public void setFieldOfView(float fov) {
    this.hasFov = true;
    this.fieldOfView = fov;
  }

  @Override
  public void setFieldOfViewModifier(float modifier) {
    this.fieldOfViewModifier = modifier;
  }
}
