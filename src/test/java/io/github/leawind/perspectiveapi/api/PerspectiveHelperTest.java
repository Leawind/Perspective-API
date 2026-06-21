package io.github.leawind.perspectiveapi.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.testutils.TestUtils;
import io.github.leawind.perspectiveapi.testutils.TestWithCamera;
import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;
import org.joml.Vector2f;
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
    @Test
    void testEulerToQuat() {

      for (float xRot = -80; xRot < 80; xRot += 7.3f) {
        for (float yRot = -179; yRot < 179; yRot += 7.3f) {
          // Euler -> Quat
          cameraAccessor.invokeSetRotation(yRot, xRot);
          Quaternionf expected = cameraAccessor.getRotation();

          Quaternionf actual =
              PerspectiveHelper.getRotation(new Vector2f(xRot, yRot), new Quaternionf());

          TestUtils.assertQuatEquals(expected, actual);
        }
      }
    }
    
    @Test
    void testQuatToEuler() {
      
    }
  }

  // Fix 1: Relax floating-point precision tolerance to 0.0001 degrees to avoid
  // failures caused by tiny truncation errors from Math.PI conversion
  private static final float DELTA = 1e-4f;

  /**
   * Helper method: compares two angles for equality, automatically handling 360-degree wrap issues
   * (e.g., -180 and 180 are considered equal)
   */
  private void assertAngleEquals(float expected, float actual, float delta, String message) {
    float diff = Math.abs(expected - actual) % 360.0f;
    if (diff > 180.0f) {
      diff = 360.0f - diff;
    }
    assertTrue(
        diff <= delta,
        message
            + String.format(" (Expected: %.4f, Actual: %.4f, Diff: %.4f)", expected, actual, diff));
  }

  // ==========================================
  // Basic direction vector tests
  // ==========================================

  @Test
  void testGetUpVector_Identity() {
    Quaternionf rotation = new Quaternionf();
    Vector3f dest = new Vector3f();
    PerspectiveHelper.getUpVector(rotation, dest);

    assertEquals(0.0f, dest.x, DELTA);
    assertEquals(1.0f, dest.y, DELTA);
    assertEquals(0.0f, dest.z, DELTA);
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

        Vector2f orientation = new Vector2f();
        PerspectiveHelper.getOrientation(original, orientation);

        Quaternionf reconstructed = new Quaternionf();
        PerspectiveHelper.getRotation(orientation, reconstructed);

        assertTrue(
            original.equals(reconstructed, DELTA),
            String.format("Round trip failed for Yaw: %.2f, Pitch: %.2f", yawDeg, pitchDeg));
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
    PerspectiveHelper.getOrientation(original, tempVector);

    Vec2 mcVec2 = new Vec2(tempVector.x, tempVector.y);

    Quaternionf reconstructed = new Quaternionf();
    PerspectiveHelper.getRotation(mcVec2, reconstructed);

    assertTrue(original.equals(reconstructed, DELTA), "Round trip with Minecraft Vec2 failed");
  }

  // ==========================================
  // ViewVector (line of sight vector) related tests
  // ==========================================

  @Test
  void testGetViewVector_FromOrientation_MCLogic() {
    // Scenario 1: Pitch=0, Yaw=0 (facing south +Z)
    Vector2f ori1 = new Vector2f(0.0f, 0.0f);
    Vector3f vec1 = new Vector3f();
    PerspectiveHelper.getViewVector(ori1, vec1);
    assertEquals(0.0f, vec1.x, DELTA);
    assertEquals(0.0f, vec1.y, DELTA);
    assertEquals(1.0f, vec1.z, DELTA);

    // Scenario 2: Pitch=-90 (looking straight up +Y), Yaw=0
    Vector2f ori2 = new Vector2f(-90.0f, 0.0f);
    Vector3f vec2 = new Vector3f();
    PerspectiveHelper.getViewVector(ori2, vec2);
    assertEquals(0.0f, vec2.x, DELTA);
    assertEquals(1.0f, vec2.y, DELTA);
    assertEquals(0.0f, vec2.z, DELTA);

    // Scenario 3: Pitch=0, Yaw=90 (facing west -X)
    Vector2f ori3 = new Vector2f(0.0f, 90.0f);
    Vector3f vec3 = new Vector3f();
    PerspectiveHelper.getViewVector(ori3, vec3);
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
        PerspectiveHelper.getViewVector(originalOrientation, viewVector);

        Vector2f reconstructedOrientation = new Vector2f();
        PerspectiveHelper.getOrientation(viewVector, reconstructedOrientation);

        assertEquals(
            originalOrientation.x,
            reconstructedOrientation.x,
            DELTA,
            String.format("Pitch mismatch for Yaw: %.2f, Pitch: %.2f", yawDeg, pitchDeg));

        // Fix 2: Use custom assertAngleEquals for Yaw to handle wrap issues between -180 and 180
        assertAngleEquals(
            originalOrientation.y,
            reconstructedOrientation.y,
            DELTA,
            String.format("Yaw mismatch for Yaw: %.2f, Pitch: %.2f", yawDeg, pitchDeg));
      }
    }
  }

  @Test
  void testGetViewVector_FromQuaternion() {
    float yawDeg = 45.0f;
    float pitchDeg = -30.0f;

    Vector2f orientation = new Vector2f(pitchDeg, yawDeg);

    // Fix 3: Must use PerspectiveHelper.getRotation to construct the quaternion.
    // Using JOML's rotationYXZ directly would lose the 180° Y-axis rotation offset
    // in the MC coordinate system where Yaw=0 corresponds to JOML's Y-axis rotation of 180°.
    Quaternionf rotation = new Quaternionf();
    PerspectiveHelper.getRotation(orientation, rotation);

    Vector3f vecFromQuat = new Vector3f();
    Vector3f vecFromOri = new Vector3f();

    PerspectiveHelper.getViewVector(rotation, vecFromQuat);
    PerspectiveHelper.getViewVector(orientation, vecFromOri);

    assertEquals(vecFromOri.x, vecFromQuat.x, DELTA);
    assertEquals(vecFromOri.y, vecFromQuat.y, DELTA);
    assertEquals(vecFromOri.z, vecFromQuat.z, DELTA);
  }
}
