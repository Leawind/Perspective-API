package io.github.leawind.perspectiveapi.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

public final class TestUtils {
  private TestUtils() {}

  private static final float DELTA = 1e-5f;

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

  public static void assertQuatEquals(Quaternionfc expected, Quaternionfc actual) {
    assertQuatEquals(expected, actual, DELTA);
  }

  public static void assertQuatEquals(Quaternionfc expected, Quaternionfc actual, float delta) {
    assertEquals(expected.x(), actual.x(), delta);
    assertEquals(expected.y(), actual.y(), delta);
    assertEquals(expected.z(), actual.z(), delta);
    assertEquals(expected.w(), actual.w(), delta);
  }

  public static Stream<Vector2fc> degrees(float xStep, float yStep) {
    var list = new ArrayList<Vector2fc>();
    for (float xRot = -80; xRot < 80; xRot += xStep) {
      for (float yRot = -179; yRot < 179; yRot += yStep) {
        list.add(new Vector2f(xRot, yRot));
      }
    }
    return list.stream();
  }
}
