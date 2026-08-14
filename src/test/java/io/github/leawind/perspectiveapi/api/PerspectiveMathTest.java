package io.github.leawind.perspectiveapi.api;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.perspectiveapi.testutils.TestUtils;
import java.util.List;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class PerspectiveMathTest {
  @Test
  void zeroRotationUsesDocumentedBasis() {
    Quaternionf identity = new Quaternionf();

    TestUtils.assertVectorEquals(
        PerspectiveMath.FORWARD, PerspectiveMath.getForward(identity, new Vector3f()));
    TestUtils.assertVectorEquals(
        PerspectiveMath.BACKWARD, PerspectiveMath.getBackward(identity, new Vector3f()));
    TestUtils.assertVectorEquals(
        PerspectiveMath.LEFT, PerspectiveMath.getLeft(identity, new Vector3f()));
    TestUtils.assertVectorEquals(
        PerspectiveMath.RIGHT, PerspectiveMath.getRight(identity, new Vector3f()));
    TestUtils.assertVectorEquals(
        PerspectiveMath.UP, PerspectiveMath.getUp(identity, new Vector3f()));
    TestUtils.assertVectorEquals(
        PerspectiveMath.DOWN, PerspectiveMath.getDown(identity, new Vector3f()));
  }

  @Test
  void zeroPointConversionsProduceIdentityAndZeroAngles() {
    Vector2f eulerZero2 = new Vector2f();
    Vector3f eulerZero3 = new Vector3f();
    Quaternionf identity = new Quaternionf();

    TestUtils.assertQuatEquals(
        identity, PerspectiveMath.eulerRadToQuat(eulerZero2, new Quaternionf()));
    TestUtils.assertQuatEquals(
        identity, PerspectiveMath.eulerDegToQuat(eulerZero2, new Quaternionf()));
    TestUtils.assertQuatEquals(
        identity, PerspectiveMath.eulerRadToQuat(eulerZero3, new Quaternionf()));
    TestUtils.assertQuatEquals(
        identity, PerspectiveMath.eulerDegToQuat(eulerZero3, new Quaternionf()));
    TestUtils.assertAngleEquals(
        eulerZero2, PerspectiveMath.quatToEulerRad(identity, new Vector2f()));
    TestUtils.assertAngleEquals(
        eulerZero2, PerspectiveMath.quatToEulerDeg(identity, new Vector2f()));
    TestUtils.assertAngleEquals(
        eulerZero3, PerspectiveMath.quatToEulerRad(identity, new Vector3f()));
    TestUtils.assertAngleEquals(
        eulerZero3, PerspectiveMath.quatToEulerDeg(identity, new Vector3f()));
  }

  @Test
  void knownEulerAnglesProduceExpectedDirections() {
    TestUtils.assertVectorEquals(
        PerspectiveMath.FORWARD, PerspectiveMath.eulerDegToDirection(0.0f, 0.0f, new Vector3f()));
    TestUtils.assertVectorEquals(
        PerspectiveMath.RIGHT, PerspectiveMath.eulerDegToDirection(0.0f, 90.0f, new Vector3f()));
    TestUtils.assertVectorEquals(
        PerspectiveMath.LEFT, PerspectiveMath.eulerDegToDirection(0.0f, -90.0f, new Vector3f()));
    TestUtils.assertVectorEquals(
        PerspectiveMath.DOWN, PerspectiveMath.eulerDegToDirection(90.0f, 0.0f, new Vector3f()));
    TestUtils.assertVectorEquals(
        PerspectiveMath.UP, PerspectiveMath.eulerDegToDirection(-90.0f, 0.0f, new Vector3f()));
  }

  @Test
  void quaternionAndEulerDirectionConversionsAgree() {
    for (Vector3f eulerDeg : representativeEulerAngles()) {
      Quaternionf quaternion = PerspectiveMath.eulerDegToQuat(eulerDeg, new Quaternionf());
      Vector3f fromEuler = PerspectiveMath.eulerDegToDirection(eulerDeg, new Vector3f());
      Vector3f fromQuaternion = PerspectiveMath.quatToDirection(quaternion, new Vector3f());

      TestUtils.assertVectorEquals(fromEuler, fromQuaternion);
      TestUtils.assertVectorEquals(
          fromQuaternion, PerspectiveMath.getForward(quaternion, new Vector3f()));
      TestUtils.assertVectorEquals(
          new Vector3f(fromQuaternion).negate(),
          PerspectiveMath.getBackward(quaternion, new Vector3f()));
    }
  }

  @Test
  void eulerQuaternionRoundTripPreservesRepresentativeRotations() {
    for (Vector3f eulerDeg : representativeEulerAngles()) {
      Quaternionf quaternion = PerspectiveMath.eulerDegToQuat(eulerDeg, new Quaternionf());
      Vector3f convertedEulerDeg = PerspectiveMath.quatToEulerDeg(quaternion, new Vector3f());
      Quaternionf convertedQuaternion =
          PerspectiveMath.eulerDegToQuat(convertedEulerDeg, new Quaternionf());

      TestUtils.assertQuatEquals(quaternion, convertedQuaternion);
    }
  }

  @Test
  void eulerQuaternionRoundTripPreservesRotationsAtGimbalLock() {
    for (float pitchDeg : new float[] {-90.0f, 90.0f}) {
      for (float yawDeg : new float[] {-170.0f, -90.0f, 0.0f, 90.0f, 170.0f}) {
        for (float rollDeg : new float[] {-150.0f, -30.0f, 0.0f, 45.0f, 160.0f}) {
          Quaternionf quaternion =
              PerspectiveMath.eulerDegToQuat(pitchDeg, yawDeg, rollDeg, new Quaternionf());
          assertCanonicalGimbalLockRoundTrip(quaternion);
          assertCanonicalGimbalLockRoundTrip(
              new Quaternionf(-quaternion.x(), -quaternion.y(), -quaternion.z(), -quaternion.w()));
        }
      }
    }
  }

  @Test
  void directionEulerRoundTripPreservesDirection() {
    for (Vector3f eulerDeg : representativeEulerAngles()) {
      Vector3f direction = PerspectiveMath.eulerDegToDirection(eulerDeg, new Vector3f());
      Vector2f convertedEulerDeg = PerspectiveMath.directionToEulerDeg(direction, new Vector2f());
      Vector3f convertedDirection =
          PerspectiveMath.eulerDegToDirection(convertedEulerDeg, new Vector3f());

      TestUtils.assertVectorEquals(direction, convertedDirection);
      TestUtils.assertQuatEquals(
          PerspectiveMath.directionToQuat(direction, new Quaternionf()),
          PerspectiveMath.eulerDegToQuat(convertedEulerDeg, new Quaternionf()));
    }
  }

  @Test
  void conversionMethodsReturnProvidedDestinations() {
    Quaternionf quaternion = new Quaternionf();
    Vector2f euler2 = new Vector2f();
    Vector3f euler3 = new Vector3f();
    Vector3f direction = new Vector3f();

    assertSame(quaternion, PerspectiveMath.eulerDegToQuat(10.0f, 20.0f, 30.0f, quaternion));
    assertSame(euler2, PerspectiveMath.quatToEulerDeg(quaternion, euler2));
    assertSame(euler3, PerspectiveMath.quatToEulerDeg(quaternion, euler3));
    assertSame(direction, PerspectiveMath.quatToDirection(quaternion, direction));
  }

  @Test
  void requiredReferenceArgumentsRejectNull() {
    assertThrows(
        NullPointerException.class,
        () -> PerspectiveMath.eulerDegToQuat((Vector3f) null, new Quaternionf()));
    assertThrows(
        NullPointerException.class, () -> PerspectiveMath.eulerDegToQuat(new Vector3f(), null));
    assertThrows(
        NullPointerException.class,
        () -> PerspectiveMath.directionToEulerDeg((Vector3f) null, new Vector3f()));
    assertThrows(
        NullPointerException.class, () -> PerspectiveMath.quatToDirection(null, new Vector3f()));
  }

  private static List<Vector3f> representativeEulerAngles() {
    return List.of(
        new Vector3f(),
        new Vector3f(20.0f, 35.0f, 0.0f),
        new Vector3f(-40.0f, 120.0f, 15.0f),
        new Vector3f(70.0f, -160.0f, -45.0f));
  }

  private static void assertCanonicalGimbalLockRoundTrip(Quaternionf quaternion) {
    Vector3f convertedEulerDeg = PerspectiveMath.quatToEulerDeg(quaternion, new Vector3f());
    Vector2f convertedEulerDeg2 = PerspectiveMath.quatToEulerDeg(quaternion, new Vector2f());
    Vector3f convertedEulerRad = PerspectiveMath.quatToEulerRad(quaternion, new Vector3f());
    Vector2f convertedEulerRad2 = PerspectiveMath.quatToEulerRad(quaternion, new Vector2f());
    Quaternionf convertedQuaternion =
        PerspectiveMath.eulerDegToQuat(convertedEulerDeg, new Quaternionf());

    TestUtils.assertAngleEquals(0.0f, convertedEulerDeg.z(), 1e-4f);
    TestUtils.assertAngleEquals(
        new Vector2f(convertedEulerDeg.x(), convertedEulerDeg.y()), convertedEulerDeg2, 1e-4f);
    TestUtils.assertAngleEquals(
        new Vector2f(convertedEulerRad.x(), convertedEulerRad.y()), convertedEulerRad2, 1e-6f);
    TestUtils.assertQuatEquals(quaternion, convertedQuaternion);
  }
}
