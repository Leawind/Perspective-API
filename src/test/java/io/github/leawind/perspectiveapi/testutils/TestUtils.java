package io.github.leawind.perspectiveapi.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.stream.Stream;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class TestUtils {
  private TestUtils() {}

  private static final float DELTA = 1e-4f;
  private static final float DEGREE_DELTA = 0.1f;

  public static void assertVectorEquals(Vector3dc expected, Vector3dc actual) {
    assertVectorEquals(expected, actual, DELTA);
  }

  public static void assertVectorEquals(Vector3dc expected, Vector3dc actual, float delta) {
    assertEquals(expected.x(), actual.x(), delta);
    assertEquals(expected.y(), actual.y(), delta);
    assertEquals(expected.z(), actual.z(), delta);
  }

  public static void assertVectorEquals(Vector3fc expected, Vector3fc actual) {
    assertVectorEquals(expected, actual, DELTA);
  }

  public static void assertVectorEquals(Vector3fc expected, Vector3fc actual, float delta) {
    assertEquals(expected.x(), actual.x(), delta);
    assertEquals(expected.y(), actual.y(), delta);
    assertEquals(expected.z(), actual.z(), delta);
  }

  public static void assertVectorEquals(Vector2fc expected, Vector2fc actual) {
    assertVectorEquals(expected, actual, DELTA);
  }

  public static void assertVectorEquals(Vector2fc expected, Vector2fc actual, float delta) {
    assertEquals(expected.x(), actual.x(), delta);
    assertEquals(expected.y(), actual.y(), delta);
  }

  public static void assertQuatEquals(Quaternionfc a, Quaternionfc b) {
    assertQuatEquals(a, b, DEGREE_DELTA);
  }

  public static void assertQuatEquals(Quaternionfc a, Quaternionfc b, float angleDegDelta) {
    float dot = Math.abs(a.x() * b.x() + a.y() * b.y() + a.z() * b.z() + a.w() * b.w());
    float angleDeg = (float) Math.toDegrees(2.0 * Math.acos(Math.min(dot, 1.0)));
    assertTrue(
        angleDeg <= angleDegDelta,
        String.format("Quaternion angle mismatch: %.6f degrees > %.6f", angleDeg, angleDegDelta));
  }

  public static Stream<Vector2fc> eulerDegs(float xStep, float yStep) {
    var list = new ArrayList<Vector2fc>();
    for (float xRot = -80; xRot < 80; xRot += xStep) {
      for (float yRot = -179; yRot < 179; yRot += yStep) {
        list.add(new Vector2f(xRot, yRot));
      }
    }
    return list.stream();
  }

  public static Stream<Vector3fc> eulerDegs(float xStep, float yStep, float rollStep) {
    var list = new ArrayList<Vector3fc>();
    for (float xRot = -80; xRot < 80; xRot += xStep) {
      for (float yRot = -179; yRot < 179; yRot += yStep) {
        for (float roll = -179; roll < 179; roll += rollStep) {
          list.add(new Vector3f(xRot, yRot, roll));
        }
      }
    }
    return list.stream();
  }

  /**
   * Helper method: compares two angles for equality, automatically handling 360-degree wrap issues
   * (e.g., -180 and 180 are considered equal)
   */
  public static void assertAngleEquals(float expected, float actual, float delta) {
    float diff = Math.abs(expected - actual) % 360.0f;
    if (diff > 180.0f) {
      diff = 360.0f - diff;
    }
    assertTrue(
        diff <= delta,
        String.format(" (Expected: %.4f, Actual: %.4f, Diff: %.4f)", expected, actual, diff));
  }

  public static void assertAngleEquals(float expected, float actual) {
    assertAngleEquals(expected, actual, 1e-4f);
  }
}
