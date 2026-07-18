package io.github.leawind.perspectiveapi.api;

import static org.junit.jupiter.api.Assertions.*;

import io.github.leawind.perspectiveapi.testutils.TestUtils;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PerspectiveMathTest {

  @Test
  void testZeroPoint() {
    Vector2f eulerZero2 = new Vector2f();
    Vector3f eulerZero3 = new Vector3f();
    Quaternionf quatZero = new Quaternionf();

    TestUtils.assertQuatEquals(
        quatZero, PerspectiveMath.eulerRadToQuat(eulerZero2, new Quaternionf()));
    TestUtils.assertQuatEquals(
        quatZero, PerspectiveMath.eulerDegToQuat(eulerZero2, new Quaternionf()));
    TestUtils.assertQuatEquals(
        quatZero, PerspectiveMath.eulerRadToQuat(eulerZero3, new Quaternionf()));
    TestUtils.assertQuatEquals(
        quatZero, PerspectiveMath.eulerDegToQuat(eulerZero3, new Quaternionf()));

    TestUtils.assertAngleEquals(eulerZero2, PerspectiveMath.toEulerRad(quatZero, new Vector2f()));
    TestUtils.assertAngleEquals(eulerZero2, PerspectiveMath.toEulerDeg(quatZero, new Vector2f()));
    TestUtils.assertAngleEquals(eulerZero3, PerspectiveMath.toEulerRad(quatZero, new Vector3f()));
    TestUtils.assertAngleEquals(eulerZero3, PerspectiveMath.toEulerDeg(quatZero, new Vector3f()));
  }

  @Nested
  class EulerAngleTest {}
}
