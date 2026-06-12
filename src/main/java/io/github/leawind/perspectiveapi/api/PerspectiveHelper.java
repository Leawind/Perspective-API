package io.github.leawind.perspectiveapi.api;

import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/// Provides utilities for converting between rotation, orientation, and view vector in Minecraft's
// coordinate system.
public final class PerspectiveHelper {
  private PerspectiveHelper() {}

  // region const fields

  private static final Vector3fc FORWARD = new Vector3f(0.0F, 0.0F, -1.0F);
  private static final Vector3fc BACKWARD = new Vector3f(0.0F, 0.0F, 1.0F);
  private static final Vector3fc UP = new Vector3f(0.0F, 1.0F, 0.0F);
  private static final Vector3fc DOWN = new Vector3f(0.0F, -1.0F, 0.0F);
  private static final Vector3fc LEFT = new Vector3f(-1.0F, 0.0F, 0.0F);
  private static final Vector3fc RIGHT = new Vector3f(1.0F, 0.0F, 0.0F);

  private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;
  private static final float RAD_TO_DEG = 180.0F / (float) Math.PI;

  // endregion

  // region local vector

  /// Transforms the forward direction by the given rotation.
  public static Vector3f getForwardVector(Quaternionfc rotation, Vector3f dest) {
    return rotation.transform(FORWARD, dest);
  }

  /// Transforms the backward direction by the given rotation.
  public static Vector3f getBackwardVector(Quaternionfc rotation, Vector3f dest) {
    return rotation.transform(BACKWARD, dest);
  }

  /// Transforms the up direction by the given rotation.
  public static Vector3f getUpVector(Quaternionfc rotation, Vector3f dest) {
    return rotation.transform(UP, dest);
  }

  /// Transforms the down direction by the given rotation.
  public static Vector3f getDownVector(Quaternionfc rotation, Vector3f dest) {
    return rotation.transform(DOWN, dest);
  }

  /// Transforms the left direction by the given rotation.
  public static Vector3f getLeftVector(Quaternionfc rotation, Vector3f dest) {
    return rotation.transform(LEFT, dest);
  }

  /// Transforms the right direction by the given rotation.
  public static Vector3f getRightVector(Quaternionfc rotation, Vector3f dest) {
    return rotation.transform(RIGHT, dest);
  }

  // endregion

  // region view vector

  /// Computes the view vector from a quaternion rotation.
  public static Vector3f getViewVector(Quaternionfc rotation, Vector3f dest) {
    return rotation.transform(FORWARD, dest);
  }

  /// Computes the view vector from JOML orientation angles.
  /// @param orientation The orientation as (pitch, yaw) in degrees.
  public static Vector3f getViewVector(Vector2fc orientation, Vector3f dest) {
    float pitchRad = orientation.x() * DEG_TO_RAD;
    float yawRad = orientation.y() * DEG_TO_RAD;

    float cosPitch = (float) Math.cos(pitchRad);
    float sinPitch = (float) Math.sin(pitchRad);
    float cosYaw = (float) Math.cos(yawRad);
    float sinYaw = (float) Math.sin(yawRad);

    // Corresponds to Minecraft's native calculateViewVector logic
    return dest.set(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
  }

  /// Computes the view vector from Minecraft's native orientation angles.
  /// @param orientation The orientation as (pitch, yaw) in degrees.
  public static Vector3f getViewVector(Vec2 orientation, Vector3f dest) {
    return getViewVector(new Vector2f(orientation.x, orientation.y), dest);
  }

  // endregion

  // region orientation

  /// Extracts orientation angles from a quaternion rotation.
  /// @return The orientation as (pitch, yaw) in degrees.
  public static Vector2f getOrientation(Quaternionfc rotation, Vector2f dest) {
    final Vector3f eulerAngles = new Vector3f();
    rotation.getEulerAnglesYXZ(eulerAngles);

    // eulerAngles.x is Pitch, eulerAngles.y is Yaw (in radians)
    return dest.set(-eulerAngles.x * RAD_TO_DEG, (float) ((Math.PI - eulerAngles.y) * RAD_TO_DEG));
  }

  /// Computes orientation angles from a view vector.
  /// @return The orientation as (pitch, yaw) in degrees.
  public static Vector2f getOrientation(Vector3fc viewVector, Vector2f dest) {
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

  // region rotation

  /// Constructs a quaternion rotation from JOML orientation angles.
  /// @param orientation The orientation as (pitch, yaw) in degrees.
  public static Quaternionf getRotation(Vector2fc orientation, Quaternionf dest) {
    float pitchRad = orientation.x() * DEG_TO_RAD;
    float yawRad = orientation.y() * DEG_TO_RAD;

    return dest.rotationYXZ((float) Math.PI - yawRad, -pitchRad, 0.0f);
  }

  /// Constructs a quaternion rotation from Minecraft's native orientation angles.
  /// @param orientation The orientation as (pitch, yaw) in degrees.
  public static Quaternionf getRotation(Vec2 orientation, Quaternionf dest) {
    return getRotation(new Vector2f(orientation.x, orientation.y), dest);
  }

  /// Constructs a quaternion rotation from a view vector.
  public static Quaternionf getRotation(Vector3fc viewVector, Quaternionf dest) {
    float x = viewVector.x();
    float y = viewVector.y();
    float z = viewVector.z();
    float horizontalLength = (float) Math.sqrt(x * x + z * z);

    // Directly compute radian Euler angles and construct quaternion to avoid precision loss and
    // temporary object allocation
    float pitchRad = (float) Math.atan2(-y, horizontalLength);
    float yawRad = (float) -Math.atan2(x, z);

    return dest.rotationYXZ((float) Math.PI - yawRad, -pitchRad, 0.0f);
  }

  // endregion
}
