package io.github.leawind.perspectiveapi.api;

import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/// ### Rotation representations
///
/// | Name         | Identifier   | Type               | Format                       |
/// | ------------ | ------------ | ------------------ | ---------------------------- |
/// | Euler Degree | `eulerDeg`   | `Vector2d`, `Vec2` | (pitch, yaw) or (xRot, yRot) |
/// | Quaternion   | `quat`       | `Quaternionf`      | (x, y, z, w)                 |
/// | View Vector  | `viewVector` | `Vector3f`         | (x, y, z)                    |
public final class PerspectiveHelper {
  private PerspectiveHelper() {}

  // region const fields
  public static final Vector3fc UP = new Vector3f(0.0F, 1.0F, 0.0F);
  public static final Vector3fc DOWN = new Vector3f(0.0F, -1.0F, 0.0F);

  /*? if >=1.21 {*/
  public static final Vector3fc FORWARD = new Vector3f(0.0F, 0.0F, -1.0F);
  public static final Vector3fc BACKWARD = new Vector3f(0.0F, 0.0F, 1.0F);
  public static final Vector3fc LEFT = new Vector3f(-1.0F, 0.0F, 0.0F);
  public static final Vector3fc RIGHT = new Vector3f(1.0F, 0.0F, 0.0F);
  /*? } else {*/
  /*public static final Vector3fc FORWARD = new Vector3f(0.0F, 0.0F, 1.0F);
  public static final Vector3fc BACKWARD = new Vector3f(0.0F, 0.0F, -1.0F);
  public static final Vector3fc LEFT = new Vector3f(1.0F, 0.0F, 0.0F);
  public static final Vector3fc RIGHT = new Vector3f(-1.0F, 0.0F, 0.0F);
  */
  /*? }*/

  private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;
  private static final float RAD_TO_DEG = 180.0F / (float) Math.PI;

  // endregion

  // region local directional vector
  /// Transforms the forward direction by the given rotation.
  public static Vector3f getForwardVector(Quaternionfc quat, Vector3f dest) {
    return quat.transform(FORWARD, dest);
  }

  /// Transforms the backward direction by the given rotation.
  public static Vector3f getBackwardVector(Quaternionfc quat, Vector3f dest) {
    return quat.transform(BACKWARD, dest);
  }

  /// Transforms the up direction by the given rotation.
  public static Vector3f getUpVector(Quaternionfc quat, Vector3f dest) {
    return quat.transform(UP, dest);
  }

  /// Transforms the down direction by the given rotation.
  public static Vector3f getDownVector(Quaternionfc quat, Vector3f dest) {
    return quat.transform(DOWN, dest);
  }

  /// Transforms the left direction by the given rotation.
  public static Vector3f getLeftVector(Quaternionfc quat, Vector3f dest) {
    return quat.transform(LEFT, dest);
  }

  /// Transforms the right direction by the given rotation.
  public static Vector3f getRightVector(Quaternionfc quat, Vector3f dest) {
    return quat.transform(RIGHT, dest);
  }

  // endregion

  // region view vector

  /// Computes the view vector from a quaternion rotation.
  public static Vector3f quatToViewVector(Quaternionfc quat, Vector3f dest) {
    return quat.transform(FORWARD, dest);
  }

  /// Computes the view vector from JOML orientation angles.
  ///
  /// @param xRot The pitch angle in degrees.
  /// @param yRot The yaw angle in degrees.
  public static Vector3f eulerDegToViewVector(float xRot, float yRot, Vector3f dest) {
    float pitchRad = xRot * DEG_TO_RAD;
    float yawRad = yRot * DEG_TO_RAD;

    float cosPitch = (float) Math.cos(pitchRad);
    float sinPitch = (float) Math.sin(pitchRad);
    float cosYaw = (float) Math.cos(yawRad);
    float sinYaw = (float) Math.sin(yawRad);
    // Corresponds to Minecraft's native calculateViewVector logic (World Space)
    return dest.set(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
  }

  /// Computes the view vector from JOML orientation angles.
  /// @param eulerDeg The orientation as (pitch, yaw) in degrees.
  public static Vector3f eulerDegToViewVector(Vector2fc eulerDeg, Vector3f dest) {
    return eulerDegToViewVector(eulerDeg.x(), eulerDeg.y(), dest);
  }

  /// Computes the view vector from Minecraft's native orientation angles.
  /// @param eulerDeg The orientation as (pitch, yaw) in degrees.
  public static Vector3f eulerDegToViewVector(Vec2 eulerDeg, Vector3f dest) {
    return eulerDegToViewVector(eulerDeg.x, eulerDeg.y, dest);
  }

  // endregion

  // region euler degree
  /// Extracts orientation angles from a quaternion rotation.
  /// @return The orientation as (pitch, yaw) in degrees.
  public static Vector2f quatToEulerDeg(Quaternionfc rotation, Vector2f dest) {
    // >=1.21  : rotationYXZ(PI - yaw, -pitch, 0)
    //   eulerAng.x = -pitch, eulerAng.y = PI - yaw
    // <=1.20.4: rotationYXZ(-yaw, pitch, 0)
    //   eulerAng.x = pitch, eulerAng.y = -yaw

    final Vector3f eulerAng = new Vector3f();
    rotation.getEulerAnglesYXZ(eulerAng);
    /*? if >=1.21 {*/
    return dest.set(-eulerAng.x() * RAD_TO_DEG, (float) ((Math.PI - eulerAng.y()) * RAD_TO_DEG));
    /*? } else {*/
    /*return dest.set(eulerAng.x() * RAD_TO_DEG, -eulerAng.y() * RAD_TO_DEG);
     */
    /*? }*/
  }

  /// Computes orientation angles from a view vector.
  /// @return The orientation as (pitch, yaw) in degrees.
  public static Vector2f viewViector2EulerDeg(Vector3fc viewVector, Vector2f dest) {
    float x = viewVector.x();
    float y = viewVector.y();
    float z = viewVector.z();
    float horizontalLength = (float) Math.sqrt(x * x + z * z);
    // Use atan2 for numerical stability, avoiding asin overflow
    float pitch = (float) (Math.atan2(-y, horizontalLength) * RAD_TO_DEG);
    float yaw = (float) (-Math.atan2(x, z) * RAD_TO_DEG);
    return dest.set(pitch, yaw);
  }

  // endregion

  // region quaternion

  /// Constructs a quaternion rotation from JOML orientation angles.
  ///
  /// @param xRot The pitch angle in degrees.
  /// @param yRot The yaw angle in degrees.
  public static Quaternionf eulerDegToQuat(float xRot, float yRot, Quaternionf dest) {
    /*? if >=1.21 {*/
    float pitchRad = -xRot * DEG_TO_RAD;
    float yawRad = (float) Math.PI - yRot * DEG_TO_RAD;
    /*? } else {*/
    /*float pitchRad = xRot * DEG_TO_RAD;
    float yawRad = -yRot * DEG_TO_RAD;
    */
    /*? }*/
    return dest.rotationYXZ(yawRad, pitchRad, 0.0f);
  }

  /// Constructs a quaternion rotation from JOML orientation angles.
  /// @param eulerDeg The orientation as (pitch, yaw) in degrees.
  public static Quaternionf eulerDegToQuat(Vector2fc eulerDeg, Quaternionf dest) {
    return eulerDegToQuat(eulerDeg.x(), eulerDeg.y(), dest);
  }

  /// Constructs a quaternion rotation from Minecraft's native orientation angles.
  /// @param eulerDeg The orientation as (pitch, yaw) in degrees.
  public static Quaternionf eulerDegToQuat(Vec2 eulerDeg, Quaternionf dest) {
    return eulerDegToQuat(eulerDeg.x, eulerDeg.y, dest);
  }

  /// Constructs a quaternion rotation from a view vector.
  public static Quaternionf viewVectorToQuat(Vector3fc viewVector, Quaternionf dest) {
    float x = viewVector.x();
    float y = viewVector.y();
    float z = viewVector.z();
    float horizontalLength = (float) Math.sqrt(x * x + z * z);

    float pitchRad = (float) Math.atan2(-y, horizontalLength);
    float yawRad = (float) -Math.atan2(x, z);

    /*? if >=1.21 {*/
    return dest.rotationYXZ((float) Math.PI - yawRad, -pitchRad, 0.0f);
    /*? } else {*/
    /*return dest.rotationYXZ(-yawRad, pitchRad, 0.0f);
     */
    /*? }*/
  }
  // endregion
}
