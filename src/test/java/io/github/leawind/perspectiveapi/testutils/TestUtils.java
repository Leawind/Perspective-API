package io.github.leawind.perspectiveapi.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Quaternionfc;

public final class TestUtils {
  private TestUtils() {}

  public static void assertQuatEquals(Quaternionfc expected, Quaternionfc actual) {
    assertQuatEquals(expected, actual, 1e-5f);
  }

  public static void assertQuatEquals(Quaternionfc expected, Quaternionfc actual, float delta) {
    assertEquals(expected.x(), actual.x(), delta);
    assertEquals(expected.y(), actual.y(), delta);
    assertEquals(expected.z(), actual.z(), delta);
    assertEquals(expected.w(), actual.w(), delta);
  }
}
