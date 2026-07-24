package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import java.util.function.Supplier;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThrottledPerspectiveSanitizer {
  private static final Logger LOGGER = LoggerFactory.getLogger(ThrottledPerspectiveSanitizer.class);
  private final Sanitizer.ThrottledAction throttledAction;

  public ThrottledPerspectiveSanitizer(Sanitizer.ThrottledAction throttledAction) {
    this.throttledAction = throttledAction;
  }

  public static boolean isValidFovDeg(float fovDeg) {
    return Sanitizer.isFinite(fovDeg) && fovDeg >= 0.0f && fovDeg <= 180.0f;
  }

  /// @return if `target` is valid
  public boolean sanitizePosition(
      String id, Vector3d target, Vector3dc fallback, Supplier<String> message) {
    if (Sanitizer.isFinite(target)) {
      return true;
    }
    throttledAction.run(
        id,
        () ->
            LOGGER.warn(
                "Invalid position {}, falling back to {}. Message: {}",
                target,
                fallback,
                message.get()));
    target.set(fallback);
    return false;
  }

  /// @return if `target` is valid
  public boolean sanitizeRotation(
      String id, Quaternionf target, Quaternionfc fallback, Supplier<String> message) {
    if (Sanitizer.isFinite(target)) {
      return true;
    }
    throttledAction.run(
        id,
        () ->
            LOGGER.warn(
                "Invalid rotation {}, falling back to {}. Message: {}",
                target,
                fallback,
                message.get()));
    target.set(fallback);
    return false;
  }

  /// @return `target` if valid, or `fallback` if invalid
  public float sanitizeFov(String id, float target, float fallback, Supplier<String> message) {
    if (isValidFovDeg(target)) {
      return target;
    }
    throttledAction.run(
        id,
        () -> {
          LOGGER.warn(
              "Invalid fov degrees {}, falling back to {}. Message: {}",
              target,
              fallback,
              message.get());
        });
    return fallback;
  }

  public void sanitize(
      String idPrefix,
      PerspectiveStateImpl.Mutable target,
      PerspectiveStateImpl fallback,
      Supplier<String> message) {
    sanitizePosition(idPrefix + ".position", target.position(), fallback.position(), message);
    sanitizeRotation(idPrefix + ".rotation", target.rotation(), fallback.rotation(), message);
    {
      float fovDeg =
          sanitizeFov(idPrefix + ".fov", target.getFovDeg(), fallback.getFovDeg(), message);
      target.setFovDeg(fovDeg);
    }
  }
}
