package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

/// Utility class for converting between rotation representations used for perspectives.
///
/// # Representation of Rotation
///
/// ## Euler Angles
///
/// When represented as a vector, each dimension has the following meaning:
///
/// | Dimension | Meaning | Positive Direction                |
/// | --------- | ------- | --------------------------------- |
/// | x         | Pitch   | Rotating downward                 |
/// | y         | Yaw     | Clockwise when viewed from above  |
/// | z         | Roll    | Clockwise around the view axis    |
///
/// For 2D vectors, z is treated as 0.
///
/// ### Zero Point
///
/// The orientation represented by the zero Euler angle `(pitch, yaw, roll) = (0, 0, 0)` is:
///
/// | Direction | Direction Vector |
/// | --------- | ---------------- |
/// | Forward   | `(0, 0, 1)`      |
/// | Up        | `(0, 1, 0)`      |
/// | Left      | `(1, 0, 0)`      |
///
/// ## Quaternions
///
/// ### Zero Point
///
/// The identity quaternion (imaginary part = 0, real part = 1) represents the same orientation as
/// the zero Euler angle:
///
/// | Direction | Direction Vector |
/// | --------- | ---------------- |
/// | Forward   | `(0, 0, 1)`      |
/// | Up        | `(0, 1, 0)`      |
/// | Left      | `(1, 0, 0)`      |
///
/// Unit quaternions are always used.
///
/// ### Rotation Order
///
/// When constructing a quaternion from Euler angles, the **Y X Z** order is used.
///
/// ## Unit Vectors
///
/// A unit vector pointing from the origin to a target can represent orientation, but cannot
/// represent roll.
///
/// When converting from Euler angles or quaternions to this format, roll information is lost.
@ApiStatus.Experimental
@SuppressWarnings("unused")
public final class PerspectiveMath {
  private PerspectiveMath() {}

  private static final float RAD_TO_DEG = (float) (180 / Math.PI);
  private static final float EULER_SINGULARITY_EPSILON = 1.0e-6f;

  public static final Vector3fc FORWARD = new Vector3f(0, 0, 1);
  public static final Vector3fc BACKWARD = new Vector3f(0, 0, -1);
  public static final Vector3fc LEFT = new Vector3f(1, 0, 0);
  public static final Vector3fc RIGHT = new Vector3f(-1, 0, 0);
  public static final Vector3fc UP = new Vector3f(0, 1, 0);
  public static final Vector3fc DOWN = new Vector3f(0, -1, 0);

  // region local directional vector

  public static @NonNull Vector3f getForward(@NonNull Quaternionfc quat, @NonNull Vector3f dest) {
    return quat.transform(FORWARD, dest);
  }

  public static @NonNull Vector3f getBackward(@NonNull Quaternionfc quat, @NonNull Vector3f dest) {
    return quat.transform(BACKWARD, dest);
  }

  public static @NonNull Vector3f getUp(@NonNull Quaternionfc quat, @NonNull Vector3f dest) {
    return quat.transform(UP, dest);
  }

  public static @NonNull Vector3f getDown(@NonNull Quaternionfc quat, @NonNull Vector3f dest) {
    return quat.transform(DOWN, dest);
  }

  public static @NonNull Vector3f getLeft(@NonNull Quaternionfc quat, @NonNull Vector3f dest) {
    return quat.transform(LEFT, dest);
  }

  public static @NonNull Vector3f getRight(@NonNull Quaternionfc quat, @NonNull Vector3f dest) {
    return quat.transform(RIGHT, dest);
  }

  // endregion

  // region to euler radian

  /// Converts a quaternion to Y-X-Z Euler angles in radians.
  ///
  /// At a gimbal lock, returns an equivalent canonical representation with roll set to zero and the
  /// coupled rotation folded into yaw.
  public static @NonNull Vector3f quatToEulerRad(
      @NonNull Quaternionfc rotation, @NonNull Vector3f dest) {
    getEulerAnglesYXZ(rotation, dest);
    return dest.mul(1, -1, 1);
  }

  /// Converts a quaternion to the pitch and yaw of its canonical Y-X-Z Euler representation in
  /// radians, discarding roll.
  public static @NonNull Vector2f quatToEulerRad(
      @NonNull Quaternionfc rotation, @NonNull Vector2f dest) {
    Vector3f full = quatToEulerRad(rotation, new Vector3f());
    return dest.set(full.x(), full.y());
  }

  private static Vector3f getEulerAnglesYXZ(Quaternionfc rotation, Vector3f dest) {
    float x = rotation.x();
    float y = rotation.y();
    float z = rotation.z();
    float w = rotation.w();
    float lengthSquared = rotation.lengthSquared();

    if (!(lengthSquared > 0.0f && Float.isFinite(lengthSquared))) {
      return rotation.getEulerAnglesYXZ(dest);
    }

    float yawNumerator = x * z + y * w;
    float yawDenominator = 0.5f * lengthSquared - y * y - x * x;
    float singularityThreshold = EULER_SINGULARITY_EPSILON * lengthSquared;
    if (yawNumerator * yawNumerator + yawDenominator * yawDenominator
        <= singularityThreshold * singularityThreshold) {
      float sinPitch = -2.0f * (y * z - w * x) / lengthSquared;
      float pitch = java.lang.Math.copySign((float) (Math.PI * 0.5), sinPitch);
      float yaw = Math.atan2(w * y - x * z, 0.5f * lengthSquared - y * y - z * z);
      return dest.set(pitch, yaw, 0.0f);
    }

    return rotation.getEulerAnglesYXZ(dest);
  }

  public static @NonNull Vector3f directionToEulerRad(
      float x, float y, float z, @NonNull Vector3f dest) {
    double horizontalLength = Math.sqrt(x * x + z * z);

    float pitch = (float) Math.atan2(-y, horizontalLength);
    @SuppressWarnings("SuspiciousNameCombination")
    float yaw = -Math.atan2(x, z);

    return dest.set(pitch, yaw, 0.0);
  }

  public static @NonNull Vector2f directionToEulerRad(
      float x, float y, float z, @NonNull Vector2f dest) {
    double horizontalLength = Math.sqrt(x * x + z * z);

    float pitch = (float) Math.atan2(-y, horizontalLength);
    @SuppressWarnings("SuspiciousNameCombination")
    float yaw = -Math.atan2(x, z);

    return dest.set(pitch, yaw);
  }

  public static @NonNull Vector3f directionToEulerRad(
      @NonNull Vector3fc direction, @NonNull Vector3f dest) {
    return directionToEulerRad(direction.x(), direction.y(), direction.z(), dest);
  }

  public static @NonNull Vector2f directionToEulerRad(
      @NonNull Vector3fc direction, @NonNull Vector2f dest) {
    return directionToEulerRad(direction.x(), direction.y(), direction.z(), dest);
  }

  // endregion

  // region to euler degrees

  /// Converts a quaternion to Y-X-Z Euler angles in degrees.
  ///
  /// At a gimbal lock, returns an equivalent canonical representation with roll set to zero and the
  /// coupled rotation folded into yaw.
  public static @NonNull Vector3f quatToEulerDeg(
      @NonNull Quaternionfc rotation, @NonNull Vector3f dest) {
    return quatToEulerRad(rotation, dest).mul(RAD_TO_DEG);
  }

  /// Converts a quaternion to the pitch and yaw of its canonical Y-X-Z Euler representation in
  /// degrees, discarding roll.
  public static @NonNull Vector2f quatToEulerDeg(
      @NonNull Quaternionfc rotation, @NonNull Vector2f dest) {
    return quatToEulerRad(rotation, dest).mul(RAD_TO_DEG);
  }

  public static @NonNull Vector3f directionToEulerDeg(
      float x, float y, float z, @NonNull Vector3f dest) {
    return directionToEulerRad(x, y, z, dest).mul(RAD_TO_DEG);
  }

  public static @NonNull Vector2f directionToEulerDeg(
      float x, float y, float z, @NonNull Vector2f dest) {
    return directionToEulerRad(x, y, z, dest).mul(RAD_TO_DEG);
  }

  public static @NonNull Vector3f directionToEulerDeg(
      @NonNull Vector3fc direction, @NonNull Vector3f dest) {
    return directionToEulerDeg(direction.x(), direction.y(), direction.z(), dest);
  }

  public static @NonNull Vector2f directionToEulerDeg(
      @NonNull Vector3fc direction, @NonNull Vector2f dest) {
    return directionToEulerDeg(direction.x(), direction.y(), direction.z(), dest);
  }

  // endregion

  // region to quaternion

  public static @NonNull Quaternionf eulerRadToQuat(
      float pitchRad, float yawRad, float rollRad, @NonNull Quaternionf dest) {
    return dest.rotationYXZ(-yawRad, pitchRad, rollRad);
  }

  public static @NonNull Quaternionf eulerDegToQuat(
      float pitchDeg, float yawDeg, float rollDeg, @NonNull Quaternionf dest) {
    return eulerRadToQuat(
        Math.toRadians(pitchDeg), Math.toRadians(yawDeg), Math.toRadians(rollDeg), dest);
  }

  public static @NonNull Quaternionf eulerRadToQuat(
      @NonNull Vector3fc eulerRad, @NonNull Quaternionf dest) {
    return eulerRadToQuat(eulerRad.x(), eulerRad.y(), eulerRad.z(), dest);
  }

  public static @NonNull Quaternionf eulerRadToQuat(
      @NonNull Vector2fc eulerRad, @NonNull Quaternionf dest) {
    return eulerRadToQuat(eulerRad.x(), eulerRad.y(), 0, dest);
  }

  public static @NonNull Quaternionf eulerDegToQuat(
      @NonNull Vector3fc eulerDeg, @NonNull Quaternionf dest) {
    return eulerDegToQuat(eulerDeg.x(), eulerDeg.y(), eulerDeg.z(), dest);
  }

  public static @NonNull Quaternionf eulerDegToQuat(
      @NonNull Vector2fc eulerDeg, @NonNull Quaternionf dest) {
    return eulerDegToQuat(eulerDeg.x(), eulerDeg.y(), 0, dest);
  }

  public static @NonNull Quaternionf directionToQuat(
      float x, float y, float z, @NonNull Quaternionf dest) {
    return eulerRadToQuat(directionToEulerRad(x, y, z, new Vector2f()), dest);
  }

  public static @NonNull Quaternionf directionToQuat(
      @NonNull Vector3fc direction, @NonNull Quaternionf dest) {
    return directionToQuat(direction.x(), direction.y(), direction.z(), dest);
  }

  // endregion

  // region to direction

  public static @NonNull Vector3f eulerRadToDirection(
      float pitchRad, float yawRad, @NonNull Vector3f dest) {
    float sinPitch = Math.sin(pitchRad);
    float cosPitch = Math.cosFromSin(sinPitch, pitchRad);

    float sinYaw = Math.sin(yawRad);
    float cosYaw = Math.cosFromSin(sinYaw, yawRad);

    return dest.set(-sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
  }

  public static @NonNull Vector3f eulerRadToDirection(
      @NonNull Vector3fc eulerRad, @NonNull Vector3f dest) {
    return eulerRadToDirection(eulerRad.x(), eulerRad.y(), dest);
  }

  public static @NonNull Vector3f eulerRadToDirection(
      @NonNull Vector2fc eulerRad, @NonNull Vector3f dest) {
    return eulerRadToDirection(eulerRad.x(), eulerRad.y(), dest);
  }

  public static @NonNull Vector3f eulerDegToDirection(
      float pitchDeg, float yawDeg, @NonNull Vector3f dest) {
    return eulerRadToDirection(Math.toRadians(pitchDeg), Math.toRadians(yawDeg), dest);
  }

  public static @NonNull Vector3f eulerDegToDirection(
      @NonNull Vector3fc eulerDeg, @NonNull Vector3f dest) {
    return eulerRadToDirection(Math.toRadians(eulerDeg.x()), Math.toRadians(eulerDeg.y()), dest);
  }

  public static @NonNull Vector3f eulerDegToDirection(
      @NonNull Vector2fc eulerDeg, @NonNull Vector3f dest) {
    return eulerRadToDirection(Math.toRadians(eulerDeg.x()), Math.toRadians(eulerDeg.y()), dest);
  }

  public static @NonNull Vector3f quatToDirection(
      @NonNull Quaternionfc quat, @NonNull Vector3f dest) {
    return quat.transform(FORWARD, dest);
  }

  // endregion
}
