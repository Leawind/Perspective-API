package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

public class PerspectiveStateImpl implements PerspectiveState.Mutable {
  private final Vector3d position = new Vector3d();
  private boolean hasPosition;
  private final Quaternionf rotation = new Quaternionf();
  private boolean hasRotation;
  private boolean hasFieldOfView;
  private float fieldOfView;
  private float fieldOfViewModifier = 1.0f;

  @Override
  public boolean hasPosition() {
    return hasPosition;
  }

  @Override
  public boolean hasRotation() {
    return hasRotation;
  }

  @Override
  public boolean hasFieldOfView() {
    return hasFieldOfView;
  }

  @Override
  public @NonNull Vector3dc getPosition() {
    if (!hasPosition) throw new IllegalStateException("Position is not present");
    return position;
  }

  @Override
  public @NonNull Quaternionfc getRotation() {
    if (!hasRotation) throw new IllegalStateException("Rotation is not present");
    return rotation;
  }

  @Override
  public float getFieldOfView() {
    if (!hasFieldOfView) throw new IllegalStateException("FieldOfView is not present");
    return fieldOfView;
  }

  @Override
  public float getFieldOfViewModifier() {
    return fieldOfViewModifier;
  }

  @Override
  public void setPosition(@NonNull Vector3dc pos) {
    this.position.set(pos);
    this.hasPosition = true;
  }

  @Override
  public void setRotation(@NonNull Quaternionfc rot) {
    this.rotation.set(rot);
    this.hasRotation = true;
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

  @Override
  public void clearPosition() {
    this.hasPosition = false;
  }

  @Override
  public void clearRotation() {
    this.hasRotation = false;
  }

  @Override
  public void clearFieldOfView() {
    this.hasFieldOfView = false;
  }

  @Override
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

  @Override
  public void lerp(float t, @NonNull PerspectiveState target, @NonNull Mutable dest) {
    // Position
    if (target.hasPosition()) {
      if (this.hasPosition()) {
        dest.setPosition(this.getPosition().lerp(target.getPosition(), t, new Vector3d()));
      } else {
        dest.setPosition(this.getPosition());
      }
    } else {
      dest.clearPosition();
    }

    // Rotation (slerp for quaternions)
    if (target.hasRotation()) {
      if (this.hasRotation) {
        dest.setRotation(this.getRotation().slerp(target.getRotation(), t, new Quaternionf()));
      } else {
        dest.setRotation(this.getRotation());
      }
    } else {
      dest.clearRotation();
    }

    // Field of View
    if (target.hasFieldOfView()) {
      if (this.hasFieldOfView()) {
        float startFov = this.fieldOfView;
        float endFov = target.getFieldOfView();
        dest.setFieldOfView(startFov + (endFov - startFov) * t);
      } else {
        dest.setFieldOfView(this.getFieldOfView());
      }
    } else {
      dest.clearFieldOfView();
    }

    // Modifier (always present, unconditional lerp)
    float startMod = this.fieldOfViewModifier;
    float endMod = target.getFieldOfViewModifier();
    dest.setFieldOfViewModifier(startMod + (endMod - startMod) * t);
  }
}
