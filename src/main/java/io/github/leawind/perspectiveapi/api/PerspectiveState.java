package io.github.leawind.perspectiveapi.api;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/// Represents the state of a camera perspective, including position, rotation, and field of view.
///
/// Each attribute (position, rotation, fov) can be independently present or absent.
/// Use the `hasXxx` fields to check for presence before accessing the corresponding value.
public class PerspectiveState {
  public final Vector3d position = new Vector3d();
  public boolean hasPosition;

  public final Quaternionf rotation = new Quaternionf();
  public boolean hasRotation;

  public boolean hasFieldOfView;
  public float fieldOfView;

  public float fieldOfViewModifier = 1.0f;

  /// Copies all present attributes from the source state to this state.
  /// Attributes not present in the source will be cleared in this state.
  public void set(@NonNull PerspectiveState src) {
    if (src.hasPosition) {
      position.set(src.position);
      hasPosition = true;
    } else {
      hasPosition = false;
    }

    if (src.hasRotation) {
      rotation.set(src.rotation);
      hasRotation = true;
    } else {
      hasRotation = false;
    }

    if (src.hasFieldOfView) {
      hasFieldOfView = true;
      fieldOfView = src.fieldOfView;
    } else {
      hasFieldOfView = false;
    }

    fieldOfViewModifier = src.fieldOfViewModifier;
  }

  /// Linearly interpolates from `this` towards `target` by factor `t`,
  /// storing the result in `dest`.
  ///
  /// For each attribute (position, rotation, fov):
  /// - If both `this` and `target` have it, performs lerp/slerp into dest.
  /// - If only `target` has it, copies it to dest.
  /// - If neither side has it, clears it in dest.
  ///
  /// Field of view modifier is always lerped unconditionally.
  ///
  /// @param t      interpolation factor in `[0, 1]`
  /// @param target the target state to interpolate towards
  /// @param dest   destination state to store the result
  public void lerp(float t, @NonNull PerspectiveState target, @NonNull PerspectiveState dest) {
    // Position
    if (target.hasPosition) {
      if (this.hasPosition) {
        dest.position.set(this.position.lerp(target.position, t, new Vector3d()));
        dest.hasPosition = true;
      } else {
        dest.position.set(target.position);
        dest.hasPosition = true;
      }
    } else {
      dest.hasPosition = false;
    }

    // Rotation (slerp for quaternions)
    if (target.hasRotation) {
      if (this.hasRotation) {
        dest.rotation.set(this.rotation.slerp(target.rotation, t, new Quaternionf()));
        dest.hasRotation = true;
      } else {
        dest.rotation.set(target.rotation);
        dest.hasRotation = true;
      }
    } else {
      dest.hasRotation = false;
    }

    // Field of View
    if (target.hasFieldOfView) {
      if (this.hasFieldOfView) {
        dest.fieldOfView = this.fieldOfView + (target.fieldOfView - this.fieldOfView) * t;
        dest.hasFieldOfView = true;
      } else {
        dest.fieldOfView = target.fieldOfView;
        dest.hasFieldOfView = true;
      }
    } else {
      dest.hasFieldOfView = false;
    }

    // Modifier (always present, unconditional lerp)
    dest.fieldOfViewModifier =
        this.fieldOfViewModifier + (target.fieldOfViewModifier - this.fieldOfViewModifier) * t;
  }
}
