package io.github.leawind.perspectiveapi.api;

import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

public interface PerspectiveState {

  default boolean hasPosition() {
    return false;
  }

  default boolean hasRotation() {
    return false;
  }

  default boolean hasFieldOfView() {
    return false;
  }

  /// @throws IllegalStateException if hasPosition() is false
  default @NonNull Vector3dc getPosition() {
    throw new IllegalStateException();
  }

  /// @throws IllegalStateException if hasRotation() is false
  default @NonNull Quaternionfc getRotation() {
    throw new IllegalStateException();
  }

  /// @throws IllegalStateException if hasFieldOfView() is false
  default float getFieldOfView() {
    throw new IllegalStateException();
  }

  /// Always applied. Default is 1.0f (no modification).
  default float getFieldOfViewModifier() {
    return 1.0f;
  }

  interface Mutable extends PerspectiveState {
    void setPosition(@NonNull Vector3dc position);

    void setRotation(@NonNull Quaternionfc rotation);

    void setFieldOfView(float fov);

    void setFieldOfViewModifier(float modifier);

    void clearPosition();

    void clearRotation();

    void clearFieldOfView();

    void set(@NonNull PerspectiveState src);

    /// Linearly interpolates from `this` towards `target` by factor `t`,
    /// storing the result in `deset`.
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
    void lerp(float t, @NonNull PerspectiveState target, @NonNull Mutable dest);
  }
}
