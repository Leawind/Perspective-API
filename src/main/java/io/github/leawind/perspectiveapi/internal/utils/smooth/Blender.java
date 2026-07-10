package io.github.leawind.perspectiveapi.internal.utils.smooth;

@FunctionalInterface
public interface Blender {
  /// Applies the easing function to the given input.
  float blend(float x);
}
