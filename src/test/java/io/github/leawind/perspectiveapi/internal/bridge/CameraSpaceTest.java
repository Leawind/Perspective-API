package io.github.leawind.perspectiveapi.internal.bridge;

import io.github.leawind.perspectiveapi.api.PerspectiveMath;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import org.joml.Quaternionf;
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
}
