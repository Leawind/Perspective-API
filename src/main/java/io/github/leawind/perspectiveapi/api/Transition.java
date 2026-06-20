package io.github.leawind.perspectiveapi.api;

import org.jspecify.annotations.NonNull;

/// Controls smooth camera transitions between perspectives.
///
/// When a perspective switch occurs, the camera interpolates from its previous position/rotation
/// to the new perspective's values
public interface Transition {

  /// Returns `true` if a transition is currently in progress at the given timestamp.
  boolean isInTransition(long now);
  
  /// Sets the transition duration in milliseconds.
  void setDuration(long duration);
  
  /// Sets the blending function used for easing.
  void setBlender(@NonNull Blender blender);

  /// Returns the current blending function.
  @NonNull Blender getBlender();

  /// A blending function that maps a normalized time value `[0, 1]` to an eased output `[0, 1]`.
  @FunctionalInterface
  interface Blender {

    /// Applies the easing function to the given input.
    float blend(float x);

    /// No easing: linear interpolation.
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

    /// $ x(2 - x) $
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
