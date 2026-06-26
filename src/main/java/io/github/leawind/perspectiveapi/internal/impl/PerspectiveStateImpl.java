package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

public class PerspectiveStateImpl implements PerspectiveState.Mutable {
  private final Vector3d position = new Vector3d();
  private boolean hasPosition;
  private final Quaternionf rotation = new Quaternionf();
  private boolean hasRotation;
  private boolean hasFieldOfView;
  private float fieldOfView;
  private float fieldOfViewModifier = 1.0f;

  public PerspectiveStateImpl() {}

  @Override
  public boolean hasPosition() {
    return hasPosition;
  }

  @Override
  public void setHasPosition(boolean value) {
    this.hasPosition = value;
  }

  @Override
  public @NonNull Vector3d position() {
    return position;
  }

  @Override
  public boolean hasRotation() {
    return hasRotation;
  }

  @Override
  public void setHasRotation(boolean value) {
    this.hasRotation = value;
  }

  @Override
  public @NonNull Quaternionf rotation() {
    return rotation;
  }

  @Override
  public void setHasFieldOfView(boolean value) {
    this.hasFieldOfView = value;
  }

  @Override
  public boolean hasFieldOfView() {
    return hasFieldOfView;
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
    this.hasFieldOfView = true;
    this.fieldOfView = fov;
  }

  @Override
  public void setFieldOfViewModifier(float modifier) {
    this.fieldOfViewModifier = modifier;
  }

  public void set(@NonNull PerspectiveState src) {
    if (src.hasPosition()) {
      position.set(src.getPosition());
      hasPosition = true;
    } else {
      hasPosition = false;
    }

    if (src.hasRotation()) {
      rotation.set(src.getRotation());
      hasRotation = true;
    } else {
      hasRotation = false;
    }

    if (src.hasFieldOfView()) {
      hasFieldOfView = true;
      fieldOfView = src.getFieldOfView();
    } else {
      hasFieldOfView = false;
    }

    fieldOfViewModifier = src.getFieldOfViewModifier();
  }

  public void interpolate(float t, @NonNull PerspectiveState start, @NonNull PerspectiveState end) {
    if (end.hasPosition()) {
      if (start.hasPosition()) {
        start.getPosition().lerp(end.getPosition(), t, position);
      } else {
        position.set(end.getPosition());
      }
      hasPosition = true;
    } else {
      hasPosition = false;
    }

    if (end.hasRotation()) {
      if (start.hasRotation()) {
        start.getRotation().slerp(end.getRotation(), t, rotation);
      } else {
        rotation.set(end.getRotation());
      }
      hasRotation = true;
    } else {
      hasRotation = false;
    }

    if (end.hasFieldOfView()) {
      if (start.hasFieldOfView()) {
        float maxFov = end.getFieldOfView();
        float minFov = start.getFieldOfView();
        fieldOfView = minFov + (maxFov - minFov) * t;
      } else {
        fieldOfView = end.getFieldOfView();
      }
      hasFieldOfView = true;
    }

    {
      float maxFov = end.getFieldOfViewModifier();
      float minFov = start.getFieldOfViewModifier();
      fieldOfViewModifier = minFov + (maxFov - minFov) * t;
    }
  }
}
