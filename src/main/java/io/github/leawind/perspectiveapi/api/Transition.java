package io.github.leawind.perspectiveapi.api;

import org.jspecify.annotations.NonNull;

public interface Transition {
  boolean isInTransition(long now);

  void setDuration(long duration);

  void setBlender(@NonNull Blender blender);

  @NonNull Blender getBlender();

  @FunctionalInterface
  interface Blender {
    float blend(float x);

    static float linear(float x) {
      return x;
    }

    /// $ 3x^2 - 2x^3 $
    static float easeInOut(float x) {
      return 3 * x * x - 2 * x * x * x;
    }

    /// $ x^2 $
    static float easeIn(float x) {
      return x * x;
    }

    static float easeOut(float x) {
      return x * (2 - x);
    }

    /// $ 1 - cos(PI/2 * x) $
    static float sineIn(float x) {
      return (float) (1 - Math.cos(x * 1.5707963267948966));
    }

    /// $ sin(PI/2 * x) $
    static float sineOut(float x) {
      return (float) Math.sin(x * 1.5707963267948966);
    }

    /// $ sin(PI/2 * x) ^ 2 $
    static float sineInOut(float x) {
      return (float) Math.pow(Math.sin(x * 1.5707963267948966), 2);
    }
  }
}
