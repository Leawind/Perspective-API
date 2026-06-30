package io.github.leawind.perspectiveapi.internal.utils;

/// Utility methods for camera and perspective operations.
public final class PerspectiveUtils {
  private PerspectiveUtils() {}

  /// Clamps a float value between min and max.
  ///
  /// @param value the value to clamp
  /// @param min minimum bound
  /// @param max maximum bound
  /// @return clamped value
  /// @throws IllegalArgumentException if min > max or either bound is NaN
  public static float clamp(float value, float min, float max) {
    if (!(min < max)) {
      if (Float.isNaN(min)) {
        throw new IllegalArgumentException("min is NaN");
      }
      if (Float.isNaN(max)) {
        throw new IllegalArgumentException("max is NaN");
      }
      if (Float.compare(min, max) > 0) {
        throw new IllegalArgumentException(min + " > " + max);
      }
    }
    return Math.min(max, Math.max(value, min));
  }
}
