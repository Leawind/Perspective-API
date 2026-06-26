package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveStateImpl;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

// TODO docs
public interface PerspectiveState {
  static Mutable create() {
    return new PerspectiveStateImpl();
  }

  @Nullable Vector3dc position();

  @Nullable Quaternionfc rotation();

  boolean hasFieldOfView();

  float getFieldOfView();

  float getFieldOfViewModifier();

  interface Mutable extends PerspectiveState {
    @NonNull Vector3d position();

    @NonNull Quaternionf rotation();

    void setFieldOfView(float fov);

    void setFieldOfViewModifier(float modifier);

    static Mutable set(PerspectiveState state, Mutable dest) {
      {
        var position = state.position();
        if (position != null) {
          dest.position().set(state.position());
        }
      }
      {
        var rotation = state.rotation();
        if (rotation != null) {
          dest.rotation().set(state.rotation());
        }
      }

      if (state.hasFieldOfView()) {
        dest.setFieldOfView(state.getFieldOfView());
      }

      dest.setFieldOfViewModifier(state.getFieldOfViewModifier());
      return dest;
    }

    static Mutable interpolate(
        float t, @NonNull PerspectiveState min, @NonNull PerspectiveState max, Mutable dest) {
      {
        var maxPosition = max.position();
        if (maxPosition != null) {
          var minPosition = min.position();
          if (minPosition != null) {
            minPosition.lerp(maxPosition, t, dest.position());
          } else {
            dest.position().set(maxPosition);
          }
        }
      }

      {
        var maxRotation = max.rotation();
        if (maxRotation != null) {
          var minRotation = min.rotation();
          if (minRotation != null) {
            minRotation.slerp(maxRotation, t, dest.rotation());
          } else {
            dest.rotation().set(maxRotation);
          }
        }
      }

      {
        if (max.hasFieldOfView()) {
          if (min.hasFieldOfView()) {
            float maxFov = max.getFieldOfView();
            float minFov = min.getFieldOfView();
            float fov = minFov + (maxFov - minFov) * t;
            dest.setFieldOfView(fov);
          } else {
            dest.setFieldOfView(max.getFieldOfView());
          }
        }
      }

      {
        float maxFov = max.getFieldOfViewModifier();
        float minFov = min.getFieldOfViewModifier();
        float fov = minFov + (maxFov - minFov) * t;
        dest.setFieldOfViewModifier(fov);
      }

      return dest;
    }
  }
}
