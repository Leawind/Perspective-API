package io.github.leawind.perspectiveapi.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import io.github.leawind.perspectiveapi.testutils.TestWithCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PerspectiveHelperTest {

  /*? if !fabric {*/
  /*@org.junit.jupiter.api.Disabled
   */
  /*? } */
  @Nested
  class WithCamera extends TestWithCamera {
    private static Quaternionfc toQuat_mc(Vector2fc eulerDeg) {
      var camera = new Camera();
      var cameraAccessor = CameraAccessor.of(camera);
      cameraAccessor.invokeSetRotation(eulerDeg.y(), eulerDeg.x());
      return cameraAccessor.getRotation();
    }

    private static Vector3f getForward_mc(Vector2fc eulerDeg) {
      var camera = new Camera();
      var cameraAccessor = CameraAccessor.of(camera);
      cameraAccessor.invokeSetRotation(eulerDeg.y(), eulerDeg.x());
      return cameraAccessor.perspective_api$forwards();
    }

    private static Vector3f getUp_mc(Vector2fc eulerDeg) {
      var camera = new Camera();
      var cameraAccessor = CameraAccessor.of(camera);
      cameraAccessor.invokeSetRotation(eulerDeg.y(), eulerDeg.x());
      return cameraAccessor.perspective_api$up();
    }

    private static Vector3f getLeft_mc(Vector2fc eulerDeg) {
      var camera = new Camera();
      var cameraAccessor = CameraAccessor.of(camera);
      cameraAccessor.invokeSetRotation(eulerDeg.y(), eulerDeg.x());
      return cameraAccessor.perspective_api$left();
    }

    @Test
    void testEulerToQuat() {
      // [Euler] ----> Quat
      //         -mc-> Quat
      TestUtils.eulerDegs(7.3f, 7.3f)
          .forEach(
              (eulerDeg) ->
                  TestUtils.assertQuatEquals(
                      toQuat_mc(eulerDeg),
                      PerspectiveHelper.eulerDegToQuat(eulerDeg, new Quaternionf())));
    }

    @Test
    void testEulerToForwardVector() {
      // [Euler] ----> Quat --> Forward
      //         ------mc-----> Forward
      TestUtils.eulerDegs(7.3f, 7.3f)
          .forEach(
              (eulerDeg) -> {
                var forward_mc = getForward_mc(eulerDeg);
                var quat = PerspectiveHelper.eulerDegToQuat(eulerDeg, new Quaternionf());
                var forward = PerspectiveHelper.getForwardVector(quat, new Vector3f());

                TestUtils.assertVectorEquals(forward_mc, forward, 1e-4f);
              });
    }

    @Test
    void testEulerToUpVector() {
      // [Euler] ----> Quat --> Up
      //         ------mc-----> Up
      TestUtils.eulerDegs(7.3f, 7.3f)
          .forEach(
              (eulerDeg) -> {
                var up_mc = getUp_mc(eulerDeg);
                var quat = PerspectiveHelper.eulerDegToQuat(eulerDeg, new Quaternionf());
                var up = PerspectiveHelper.getUpVector(quat, new Vector3f());

                TestUtils.assertVectorEquals(up_mc, up, 1e-4f);
              });
    }

    @Test
    void testEulerToLeftVector() {
      // [Euler] ----> Quat --> Left
      //         ------mc-----> Left
      TestUtils.eulerDegs(7.3f, 7.3f)
          .forEach(
              (eulerDeg) -> {
                var left_mc = getLeft_mc(eulerDeg);
                var quat = PerspectiveHelper.eulerDegToQuat(eulerDeg, new Quaternionf());
                var left = PerspectiveHelper.getLeftVector(quat, new Vector3f());

                TestUtils.assertVectorEquals(left_mc, left, 1e-4f);
              });
    }
  }

  private static final float DELTA = 1e-4f;

  // ==========================================
  // Basic direction vector tests
  // ==========================================

  @Test
  void testGetUpVector_Identity() {
    Quaternionf rotation = new Quaternionf();
    Vector3f dest = new Vector3f();
    PerspectiveHelper.getUpVector(rotation, dest);

    assertEquals(0.0f, dest.x());
    assertEquals(1.0f, dest.y());
    assertEquals(0.0f, dest.z());
  }

  // ==========================================
  // Rotation (Quaternion) <-> Orientation (Euler angles) round-trip tests
  // ==========================================

  @Test
  void testRoundTrip_QuaternionToVector2fAndBack() {
    float[] testYawDegrees = {0.0f, 45.0f, 90.0f, 180.0f, -135.0f};
    float[] testPitchDegrees = {0.0f, 30.0f, -45.0f, 89.0f};

    for (float yawDeg : testYawDegrees) {
      for (float pitchDeg : testPitchDegrees) {
        Quaternionf original =
            new Quaternionf()
                .rotationYXZ(
                    (float) Math.toRadians(yawDeg), (float) Math.toRadians(pitchDeg), 0.0f);
        Vector2f eulerDeg = PerspectiveHelper.quatToEulerDeg(original, new Vector2f());
        Quaternionf reconstructed = PerspectiveHelper.eulerDegToQuat(eulerDeg, new Quaternionf());

        TestUtils.assertQuatEquals(original, reconstructed);
      }
    }
  }

  @Test
  void testRoundTrip_QuaternionToVec2AndBack() {
    float yawDeg = 120.0f;
    float pitchDeg = -15.0f;

    Quaternionf original =
        new Quaternionf()
            .rotationYXZ((float) Math.toRadians(yawDeg), (float) Math.toRadians(pitchDeg), 0.0f);

    Vector2f tempVector = new Vector2f();
    PerspectiveHelper.quatToEulerDeg(original, tempVector);

    Vec2 mcVec2 = new Vec2(tempVector.x, tempVector.y);

    Quaternionf reconstructed = new Quaternionf();
    PerspectiveHelper.eulerDegToQuat(mcVec2, reconstructed);

    TestUtils.assertQuatEquals(original, reconstructed);
  }

  // ==========================================
  // ViewVector (line of sight vector) related tests
  // ==========================================

  @Test
  void testEulerDegToViewVector_FromOrientation_MCLogic() {
    // Scenario 1: Pitch=0, Yaw=0 (facing south +Z)
    Vector2f ori1 = new Vector2f(0.0f, 0.0f);
    Vector3f vec1 = new Vector3f();
    PerspectiveHelper.eulerDegToViewVector(ori1, vec1);
    assertEquals(0.0f, vec1.x, DELTA);
    assertEquals(0.0f, vec1.y, DELTA);
    assertEquals(1.0f, vec1.z, DELTA);

    // Scenario 2: Pitch=-90 (looking straight up +Y), Yaw=0
    Vector2f ori2 = new Vector2f(-90.0f, 0.0f);
    Vector3f vec2 = new Vector3f();
    PerspectiveHelper.eulerDegToViewVector(ori2, vec2);
    assertEquals(0.0f, vec2.x, DELTA);
    assertEquals(1.0f, vec2.y, DELTA);
    assertEquals(0.0f, vec2.z, DELTA);

    // Scenario 3: Pitch=0, Yaw=90 (facing west -X)
    Vector2f ori3 = new Vector2f(0.0f, 90.0f);
    Vector3f vec3 = new Vector3f();
    PerspectiveHelper.eulerDegToViewVector(ori3, vec3);
    assertEquals(-1.0f, vec3.x, DELTA);
    assertEquals(0.0f, vec3.y, DELTA);
    assertEquals(0.0f, vec3.z, DELTA);
  }

  @Test
  void testRoundTrip_OrientationToViewVectorAndBack() {
    float[] testYawDegrees = {-180.0f, -90.0f, 0.0f, 90.0f, 179.0f};
    float[] testPitchDegrees = {-89.0f, -45.0f, 0.0f, 45.0f, 89.0f};

    for (float yawDeg : testYawDegrees) {
      for (float pitchDeg : testPitchDegrees) {
        Vector2f originalOrientation = new Vector2f(pitchDeg, yawDeg);

        Vector3f viewVector = new Vector3f();
        PerspectiveHelper.eulerDegToViewVector(originalOrientation, viewVector);

        Vector2f reconstructedOrientation = new Vector2f();
        PerspectiveHelper.viewVectorToEulerDeg(viewVector, reconstructedOrientation);

        assertEquals(
            originalOrientation.x,
            reconstructedOrientation.x,
            DELTA,
            String.format("Pitch mismatch for Yaw: %.2f, Pitch: %.2f", yawDeg, pitchDeg));

        // Fix 2: Use custom assertAngleEquals for Yaw to handle wrap issues between -180 and 180
        TestUtils.assertAngleEquals(originalOrientation.y, reconstructedOrientation.y);
      }
    }
  }

  // ==========================================
  // 3-Axis Euler (Pitch, Yaw, Roll) Tests
  // ==========================================

  @Test
  void testRoundTrip_QuaternionToVector3fAndBack() {
    float[] xRots = {-89f, -45f, 0f, 45f, 89f};
    float[] yRots = {-180f, -90f, 0f, 90f, 179f};
    float[] rolls = {-180f, -90f, -45f, 0f, 45f, 90f, 180f};

    for (float xRot : xRots) {
      for (float yRot : yRots) {
        for (float zRot : rolls) {
          Quaternionf original =
              PerspectiveHelper.eulerDegToQuat(xRot, yRot, zRot, new Quaternionf());
          Vector3f euler = PerspectiveHelper.quatToEulerDeg(original, new Vector3f());
          Quaternionf reconstructed = PerspectiveHelper.eulerDegToQuat(euler, new Quaternionf());

          TestUtils.assertQuatEquals(original, reconstructed);
        }
      }
    }
  }

  @Test
  void testThreeAxisMatchesTwoAxisWhenRollZero() {
    float[] pitches = {-89f, 0f, 45f, 89f};
    float[] yaws = {-180f, -90f, 0f, 90f, 179f};

    for (float p : pitches) {
      for (float y : yaws) {
        Quaternionf quat2 = PerspectiveHelper.eulerDegToQuat(p, y, new Quaternionf());
        Quaternionf quat3 = PerspectiveHelper.eulerDegToQuat(p, y, 0f, new Quaternionf());

        assertTrue(
            quat2.equals(quat3),
            String.format("2-axis and 3-axis(roll=0) mismatch: pitch=%.2f yaw=%.2f", p, y));

        Vector2f euler2 = PerspectiveHelper.quatToEulerDeg(quat2, new Vector2f());
        Vector3f euler3 = PerspectiveHelper.quatToEulerDeg(quat2, new Vector3f());

        assertEquals(euler2.x(), euler3.x(), "pitch mismatch");
        float expected = euler2.y();
        float actual = euler3.y();
        TestUtils.assertAngleEquals(expected, actual);
        assertEquals(0f, euler3.z(), "roll should be 0");
      }
    }
  }

  @Test
  void testViewVectorIgnoresRoll() {
    float pitch = 30f;
    float yaw = 45f;
    float[] rolls = {-180f, -90f, -45f, 0f, 45f, 90f, 180f};

    Vector3f baseView = PerspectiveHelper.eulerDegToViewVector(pitch, yaw, new Vector3f());

    for (float roll : rolls) {
      Vector3f viewWithRoll =
          PerspectiveHelper.eulerDegToViewVector(new Vector3f(pitch, yaw, roll), new Vector3f());

      TestUtils.assertVectorEquals(baseView, viewWithRoll);
    }
  }

  @Test
  void testViewVectorToEuler3fSetsRollZero() {
    Vector3f viewVector = PerspectiveHelper.eulerDegToViewVector(30f, 45f, new Vector3f());
    Vector3f euler3 = PerspectiveHelper.viewVectorToEulerDeg(viewVector, new Vector3f());

    assertEquals(0f, euler3.z(), "Roll from view vector should always be 0");

    // Also verify pitch/yaw match the 2-axis version
    Vector2f euler2 = PerspectiveHelper.viewVectorToEulerDeg(viewVector, new Vector2f());
    assertEquals(euler2.x(), euler3.x(), "pitch mismatch");
    assertEquals(euler2.y(), euler3.y(), "yaw mismatch");
  }

  // ==========================================
  // normalizeEulerDeg & wrapAngle Tests
  // ==========================================

  @Test
  void testWrapAngle() {
    // Standard wrapping
    assertEquals(0f, PerspectiveHelper.wrapAngle(0f));
    assertEquals(90f, PerspectiveHelper.wrapAngle(90f));
    assertEquals(-90f, PerspectiveHelper.wrapAngle(-90f));
    assertEquals(180f, PerspectiveHelper.wrapAngle(180f));
    TestUtils.assertAngleEquals(-180f, PerspectiveHelper.wrapAngle(-180f));

    // Overflow wrapping
    TestUtils.assertAngleEquals(0f, PerspectiveHelper.wrapAngle(360f));
    TestUtils.assertAngleEquals(0f, PerspectiveHelper.wrapAngle(-360f));
    TestUtils.assertAngleEquals(10f, PerspectiveHelper.wrapAngle(370f));
    TestUtils.assertAngleEquals(-10f, PerspectiveHelper.wrapAngle(-370f));
    TestUtils.assertAngleEquals(90f, PerspectiveHelper.wrapAngle(450f));
    TestUtils.assertAngleEquals(-90f, PerspectiveHelper.wrapAngle(-450f));
  }

  @Test
  void testNormalizeEulerDeg_NormalRange() {
    // Already normalized values should remain unchanged
    Vector3f euler = new Vector3f(45f, 90f, 30f);
    PerspectiveHelper.normalizeEulerDeg(euler);
    assertEquals(45f, euler.x(), "pitch");
    float actual1 = euler.y();
    TestUtils.assertAngleEquals(90f, actual1);
    float actual = euler.z();
    TestUtils.assertAngleEquals(30f, actual);
  }

  @Test
  void testNormalizeEulerDeg_PitchOverflowPositive() {
    // pitch=100 → 80, yaw+=180, roll+=180
    Vector3f euler = new Vector3f(100f, 0f, 0f);
    PerspectiveHelper.normalizeEulerDeg(euler);
    assertEquals(80f, euler.x(), "pitch");
    float actual1 = euler.y();
    TestUtils.assertAngleEquals(180f, actual1);
    float actual = euler.z();
    TestUtils.assertAngleEquals(180f, actual);
  }

  @Test
  void testNormalizeEulerDeg_PitchOverflowNegative() {
    // pitch=-100 → -80, yaw+=180, roll+=180
    Vector3f euler = new Vector3f(-100f, 0f, 0f);
    PerspectiveHelper.normalizeEulerDeg(euler);
    assertEquals(-80f, euler.x(), "pitch");
    float actual1 = euler.y();
    TestUtils.assertAngleEquals(180f, actual1);
    float actual = euler.z();
    TestUtils.assertAngleEquals(180f, actual);
  }

  @Test
  void testNormalizeEulerDeg_YawAndRollWrapping() {
    // yaw and roll should wrap independently of pitch normalization
    Vector3f euler = new Vector3f(45f, 270f, -270f);
    PerspectiveHelper.normalizeEulerDeg(euler);
    assertEquals(45f, euler.x(), "pitch");
    float actual1 = euler.y();
    TestUtils.assertAngleEquals(-90f, actual1);
    float actual = euler.z();
    TestUtils.assertAngleEquals(90f, actual);
  }

  @Test
  void testNormalizeEulerDeg_PreservesQuaternionEquivalence() {
    // Normalization should produce an equivalent quaternion
    float[] testCases = {100f, -100f, 200f, -200f, 350f, -350f};
    for (float pitch : testCases) {
      for (float yaw : new float[] {0f, 90f, 180f, -90f}) {
        for (float roll : new float[] {0f, 45f, -45f, 180f}) {
          Vector3f euler = new Vector3f(pitch, yaw, roll);
          Quaternionf originalQuat = PerspectiveHelper.eulerDegToQuat(euler, new Quaternionf());

          PerspectiveHelper.normalizeEulerDeg(euler);
          Quaternionf normalizedQuat = PerspectiveHelper.eulerDegToQuat(euler, new Quaternionf());

          TestUtils.assertQuatEquals(originalQuat, normalizedQuat);
        }
      }
    }
  }
}
