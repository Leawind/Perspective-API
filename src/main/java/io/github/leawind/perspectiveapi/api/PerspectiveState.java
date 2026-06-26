package io.github.leawind.perspectiveapi.api;

import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

// TODO docs
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

  default @NonNull Vector3dc getPosition() {
    throw new IllegalStateException();
  }

  default @NonNull Quaternionfc getRotation() {
    throw new IllegalStateException();
  }

  default float getFieldOfView() {
    throw new IllegalStateException();
  }

  default float getFieldOfViewModifier() {
    return 1.0f;
  }

  interface Mutable extends PerspectiveState {
    void setHasPosition(boolean value);

    void setHasRotation(boolean value);

    void setHasFieldOfView(boolean value);

    @NonNull Vector3d position();

    @NonNull Quaternionf rotation();

    void setFieldOfView(float fov);

    void setFieldOfViewModifier(float modifier);
  }
}
