package io.github.leawind.perspectiveapi.internal.utils;

import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {
  public static final Logger DEBUG_LOGGER = LoggerFactory.getLogger("Debug");

  private Utils() {}

  /// Clamps a float value between min and max.
  ///
  /// Refer to `Math#clamp(float, float, float)`
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

  /// Clamps a double value between min and max.
  ///
  /// Refer to `Math#clamp(double, double, double)`
  ///
  /// @param value the value to clamp
  /// @param min minimum bound
  /// @param max maximum bound
  /// @return clamped value
  /// @throws IllegalArgumentException if min > max or either bound is NaN
  public static double clamp(double value, double min, double max) {
    if (!(min < max)) {
      if (Double.isNaN(min)) {
        throw new IllegalArgumentException("min is NaN");
      }
      if (Double.isNaN(max)) {
        throw new IllegalArgumentException("max is NaN");
      }
      if (Double.compare(min, max) > 0) {
        throw new IllegalArgumentException(min + " > " + max);
      }
    }
    return Math.min(max, Math.max(value, min));
  }

  public static boolean isClassAvailable(String className) {
    try {
      Class.forName(className, false, Thread.currentThread().getContextClassLoader());
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static String toString(Quaternionf quat) {
    return String.format("Quatf(%.4f, %.4f, %.4f, %.4f)", quat.x(), quat.y(), quat.z(), quat.w());
  }

  public static String toString(Vector3f v) {
    return String.format("Vector3f(%.4f, %.4f, %.4f)", v.x(), v.y(), v.z());
  }

  public static String toString(Vector2f v) {
    return String.format("Vector2f(%.4f, %.4f)", v.x(), v.y());
  }
}
