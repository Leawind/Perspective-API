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
    assertEquals(
        16.0f,
        sanitizer.sanitizeOrthographicHeight(
            "orthographic_height", 16.0f, 8.0f, () -> message(messages)));
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
    assertEquals(
        16.0f,
        sanitizer.sanitizeOrthographicHeight("orthographic_height", 0.0f, 16.0f, () -> "test"));

    TestUtils.assertVectorEquals(fallbackPosition, position);
    TestUtils.assertQuatEquals(fallbackRotation, rotation);
  }

  @Test
  void fovValidationExcludesEndpointsAndOutOfRangeValues() {
    assertTrue(ThrottledPerspectiveSanitizer.isValidFovDeg(0.01f));
    assertTrue(ThrottledPerspectiveSanitizer.isValidFovDeg(179.99f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(0.0f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(180.0f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(-0.01f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(180.01f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(Float.NaN));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(Float.NEGATIVE_INFINITY));
    assertFalse(ThrottledPerspectiveSanitizer.isValidFovDeg(Float.POSITIVE_INFINITY));
  }

  @Test
  void orthographicHeightHasLowerBoundButNoUpperBound() {
    assertTrue(
        ThrottledPerspectiveSanitizer.isValidOrthographicHeight(
            ThrottledPerspectiveSanitizer.MIN_ORTHOGRAPHIC_HEIGHT));
    assertTrue(ThrottledPerspectiveSanitizer.isValidOrthographicHeight(Float.MAX_VALUE));
    assertFalse(
        ThrottledPerspectiveSanitizer.isValidOrthographicHeight(
            Math.nextDown(ThrottledPerspectiveSanitizer.MIN_ORTHOGRAPHIC_HEIGHT)));
    assertFalse(ThrottledPerspectiveSanitizer.isValidOrthographicHeight(0.0f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidOrthographicHeight(-1.0f));
    assertFalse(ThrottledPerspectiveSanitizer.isValidOrthographicHeight(Float.POSITIVE_INFINITY));
  }

  @Test
  void rotationMustBeFiniteAndUnitLength() {
    Quaternionf fallback = new Quaternionf().rotationY(0.5f);
    Quaternionf zero = new Quaternionf(0.0f, 0.0f, 0.0f, 0.0f);
    Quaternionf scaled = new Quaternionf(0.0f, 0.0f, 0.0f, 2.0f);

    assertFalse(sanitizer.sanitizeRotation("zero", zero, fallback, () -> "test"));
    assertFalse(sanitizer.sanitizeRotation("scaled", scaled, fallback, () -> "test"));
    TestUtils.assertQuatEquals(fallback, zero);
    TestUtils.assertQuatEquals(fallback, scaled);
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
    target.setOrthographicHeight(Float.NaN);

    sanitizer.sanitize("test", target, fallback, () -> "test");

    TestUtils.assertVectorEquals(fallback.position(), target.position());
    TestUtils.assertQuatEquals(new Quaternionf().rotationX(0.5f), target.rotation());
    assertEquals(100.0f, target.getFovDeg());
    assertEquals(fallback.getOrthographicHeight(), target.getOrthographicHeight());
  }

  private static String message(AtomicInteger count) {
    count.incrementAndGet();
    return "test";
  }
}
