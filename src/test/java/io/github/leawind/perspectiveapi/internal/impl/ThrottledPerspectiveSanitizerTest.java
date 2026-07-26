package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import java.util.concurrent.atomic.AtomicInteger;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ThrottledPerspectiveSanitizerTest {
  private ThrottledPerspectiveSanitizer sanitizer;

  @BeforeEach
  void beforeEach() {
    sanitizer = new ThrottledPerspectiveSanitizer(new Sanitizer.ThrottledAction(Long.MAX_VALUE));
  }

  @Test
  void acceptsValidValuesWithoutEvaluatingMessage() {
    AtomicInteger messages = new AtomicInteger();
    Vector3d position = new Vector3d(1.0, 2.0, 3.0);
    Quaternionf rotation = new Quaternionf().rotationXYZ(0.1f, 0.2f, 0.3f);

    assertTrue(
        sanitizer.sanitizePosition("position", position, new Vector3d(), () -> message(messages)));
    assertTrue(
        sanitizer.sanitizeRotation(
            "rotation", rotation, new Quaternionf(), () -> message(messages)));
    assertEquals(90.0f, sanitizer.sanitizeFovDeg("fov", 90.0f, 70.0f, () -> message(messages)));
    assertEquals(0, messages.get());
  }

  @Test
  void invalidValuesAreReplacedByFallbacks() {
    Vector3d position = new Vector3d(Double.NaN, 2.0, 3.0);
    Vector3d fallbackPosition = new Vector3d(4.0, 5.0, 6.0);
    Quaternionf rotation = new Quaternionf(Float.NaN, 0.0f, 0.0f, 1.0f);
    Quaternionf fallbackRotation = new Quaternionf().rotationY(0.5f);

    assertFalse(sanitizer.sanitizePosition("position", position, fallbackPosition, () -> "test"));
    assertFalse(sanitizer.sanitizeRotation("rotation", rotation, fallbackRotation, () -> "test"));
    assertEquals(70.0f, sanitizer.sanitizeFovDeg("fov", Float.NaN, 70.0f, () -> "test"));

    TestUtils.assertVectorEquals(fallbackPosition, position);
    TestUtils.assertQuatEquals(fallbackRotation, rotation);
  }

  @Test
  void fovValidationIncludesEndpointsAndRejectsOutOfRangeValues() {
    assertTrue(ThrottledPerspectiveSanitizer.isValidFovDeg(0.0f));
    assertTrue(ThrottledPerspectiveSanitizer.isValidFovDeg(180.0f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(-0.01f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(180.01f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(Float.POSITIVE_INFINITY));
  }

  @Test
  void sanitizeRestoresOnlyInvalidFields() {
    PerspectiveStateImpl fallback = new PerspectiveStateImpl();
    fallback.position().set(1.0, 2.0, 3.0);
    fallback.rotation().rotationY(0.25f);
    fallback.setFovDeg(70.0f);
    PerspectiveStateImpl target = new PerspectiveStateImpl().set(fallback);
    target.position().x = Double.POSITIVE_INFINITY;
    target.rotation().rotationX(0.5f);
    target.setFovDeg(100.0f);

    sanitizer.sanitize("test", target, fallback, () -> "test");

    TestUtils.assertVectorEquals(fallback.position(), target.position());
    TestUtils.assertQuatEquals(new Quaternionf().rotationX(0.5f), target.rotation());
    assertEquals(100.0f, target.getFovDeg());
  }

  private static String message(AtomicInteger count) {
    count.incrementAndGet();
    return "test";
  }
}
