package io.github.leawind.perspectiveapi.api;

public interface Transition {
  boolean isInTransition(long now);

  void setDuration(long duration);

  void setBlender(Blender blender);

  Blender getBlender();

  interface Blender {
    float blend(float x);

    static float linear(float x) {
      return x;
    }

    static float easeInOut(float x) {
      return 3 * x * x - 2 * x * x * x;
    }

    static float easeIn(float x) {
      return x * x;
    }

    static float easeOut(float x) {
      return x * (2 - x);
    }

    static float sineOut(float x) {
      return (float) Math.sin(x * 1.5707963267948966f);
    }
  }
}
