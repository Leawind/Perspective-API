package io.github.leawind.perspectiveapi.internal.bridge;

import io.github.leawind.perspectiveapi.api.PerspectiveMath;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class CameraSpaceTest {
  @Test
  void testRoundTrip_mc_api_mc() {
    TestUtils.eulerDegs(9.7f, 9.7f, 9.7f)
        .forEach(
            eulerDeg -> {
              var mcQuat =
                  CameraSpace.eulerDegToMcQuat(
                      eulerDeg.x(), eulerDeg.y(), eulerDeg.z(), new Quaternionf());
              var apiQuat = CameraSpace.mcToApi(mcQuat, new Quaternionf());
              var backToMcQuat = CameraSpace.apiToMc(apiQuat, new Quaternionf());

              TestUtils.assertQuatEquals(mcQuat, backToMcQuat);
            });
  }

  @Test
  void testRoundTrip_api_mc_api() {
    TestUtils.eulerDegs(9.7f, 9.7f, 9.7f)
        .forEach(
            eulerDeg -> {
              var apiQuat = PerspectiveMath.eulerDegToQuat(eulerDeg, new Quaternionf());
              var mcQuat = CameraSpace.apiToMc(apiQuat, new Quaternionf());
              var backToApiQuat = CameraSpace.mcToApi(mcQuat, new Quaternionf());

              TestUtils.assertQuatEquals(apiQuat, backToApiQuat);
            });
  }

  @Test
  void testRoundTrip_eulerDeg_mcQuat_eulerDeg() {
    TestUtils.eulerDegs(9.7f, 9.7f, 9.7f)
        .forEach(
            eulerDeg -> {
              var mcQuat =
                  CameraSpace.eulerDegToMcQuat(
                      eulerDeg.x(), eulerDeg.y(), eulerDeg.z(), new Quaternionf());
              var backToEulerDeg = CameraSpace.mcQuatToEulerDeg(mcQuat, new Vector3f());

              TestUtils.assertAngleEquals(eulerDeg, backToEulerDeg, 1e-3f);
            });
  }

  @Test
  void testRoundTrip_mcQuat_eulerDeg_mcQuat() {
    TestUtils.eulerDegs(9.7f, 9.7f, 9.7f)
        .forEach(
            eulerDegRaw -> {
              var mcQuat =
                  CameraSpace.eulerDegToMcQuat(
                      eulerDegRaw.x(), eulerDegRaw.y(), eulerDegRaw.z(), new Quaternionf());
              var eulerDeg = CameraSpace.mcQuatToEulerDeg(mcQuat, new Vector3f());
              var backToMcQuat =
                  CameraSpace.eulerDegToMcQuat(
                      eulerDeg.x(), eulerDeg.y(), eulerDeg.z(), new Quaternionf());

              TestUtils.assertQuatEquals(mcQuat, backToMcQuat);
            });
  }

  @Test
  void testRoundTripAtGimbalLock() {
    for (float pitchDeg : new float[] {-90.0f, 90.0f}) {
      for (float yawDeg : new float[] {-170.0f, -90.0f, 0.0f, 90.0f, 170.0f}) {
        for (float rollDeg : new float[] {-150.0f, -30.0f, 0.0f, 45.0f, 160.0f}) {
          Quaternionf mcQuat =
              CameraSpace.eulerDegToMcQuat(pitchDeg, yawDeg, rollDeg, new Quaternionf());
          assertCanonicalGimbalLockRoundTrip(mcQuat);
          assertCanonicalGimbalLockRoundTrip(
              new Quaternionf(-mcQuat.x(), -mcQuat.y(), -mcQuat.z(), -mcQuat.w()));
        }
      }
    }
  }

  private static void assertCanonicalGimbalLockRoundTrip(Quaternionf mcQuat) {
    Vector3f convertedEulerDeg = CameraSpace.mcQuatToEulerDeg(mcQuat, new Vector3f());
    Vector2f convertedEulerDeg2 = CameraSpace.mcQuatToEulerDeg(mcQuat, new Vector2f());
    Quaternionf convertedMcQuat =
        CameraSpace.eulerDegToMcQuat(
            convertedEulerDeg.x(), convertedEulerDeg.y(), convertedEulerDeg.z(), new Quaternionf());

    TestUtils.assertAngleEquals(0.0f, convertedEulerDeg.z(), 1e-4f);
    TestUtils.assertAngleEquals(
        new Vector2f(convertedEulerDeg.x(), convertedEulerDeg.y()), convertedEulerDeg2, 1e-4f);
    TestUtils.assertQuatEquals(mcQuat, convertedMcQuat);
  }
}
