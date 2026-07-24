package io.github.leawind.perspectiveapi.internal.utils.smooth;

public final class Blenders {
  private Blenders() {}

  /// No easing: linear interpolation.
  public static float linear(float x) {
    return x;
  }

  /// $ 3x^2 - 2x^3 $
  public static float easeInOut(float x) {
    return 3 * x * x - 2 * x * x * x;
  }

  /// $ x^2 $
  public static float easeIn(float x) {
    return x * x;
  }

  /// $ x(2 - x) $
  public static float easeOut(float x) {
    return x * (2 - x);
  }

  /// $ 1 - cos(PI/2 * x) $
  public static float sineIn(float x) {
    return (float) (1 - Math.cos(x * 1.5707963267948966));
  }

  /// $ sin(PI/2 * x) $
  public static float sineOut(float x) {
    return (float) Math.sin(x * 1.5707963267948966);
  }

  /// $ sin(PI/2 * x) ^ 2 $
  public static float sineInOut(float x) {
    return (float) Math.pow(Math.sin(x * 1.5707963267948966), 2);
  }
}
