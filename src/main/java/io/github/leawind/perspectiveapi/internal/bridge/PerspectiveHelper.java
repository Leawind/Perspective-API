package io.github.leawind.perspectiveapi.internal.bridge;

import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public final class PerspectiveHelper {
  private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;
  private static final float RAD_TO_DEG = 180.0F / (float) Math.PI;

  // region const fields
  public static final Vector3fc UP;
  public static final Vector3fc DOWN;
  public static final Vector3fc FORWARD;
  public static final Vector3fc BACKWARD;
  public static final Vector3fc LEFT;
  public static final Vector3fc RIGHT;

  // Internal decomposition uses YXZ order (matching vanilla).
  //   >=1.21  : built via rotationYXZ(PI - yaw, -pitch, roll)
  //                => eulerRad.x = -pitch, eulerRad.y = PI - yaw, eulerRad.z = roll
  //   else: built via rotationYXZ(-yaw, pitch, roll)
  //                => eulerRad.x = pitch,  eulerRad.y = -yaw, eulerRad.z = roll
  private static final float PITCH_SIGN;
  private static final float YAW_SIGN;
  private static final float YAW_OFFSET_RAD;

  static {
    UP = new Vector3f(0.0F, 1.0F, 0.0F);
    DOWN = new Vector3f(0.0F, -1.0F, 0.0F);

    /*? if >=1.21 {*/
    FORWARD = new Vector3f(0.0F, 0.0F, -1.0F);
    BACKWARD = new Vector3f(0.0F, 0.0F, 1.0F);
    LEFT = new Vector3f(-1.0F, 0.0F, 0.0F);
    RIGHT = new Vector3f(1.0F, 0.0F, 0.0F);
    PITCH_SIGN = -1.0F;
    YAW_SIGN = -1.0F;
    YAW_OFFSET_RAD = (float) Math.PI;
    /*? } else {*/
    /*FORWARD = new Vector3f(0.0F, 0.0F, 1.0F);
    BACKWARD = new Vector3f(0.0F, 0.0F, -1.0F);
    LEFT = new Vector3f(1.0F, 0.0F, 0.0F);
    RIGHT = new Vector3f(-1.0F, 0.0F, 0.0F);
    PITCH_SIGN = 1.0F;
    YAW_SIGN = -1.0F;
    YAW_OFFSET_RAD = 0.0F;
    */
    /*? }*/
  }

  // endregion

  private PerspectiveHelper() {}

  // region to quaternion

  /// Constructs a quaternion from euler angles (pitch, yaw) with roll = 0.
  ///
  /// @param xRot The pitch angle in degrees.
  /// @param yRot   The yaw angle in degrees.
  public static Quaternionf eulerDegToQuat(float xRot, float yRot, @NonNull Quaternionf dest) {
    return eulerDegToQuat(xRot, yRot, 0.0f, dest);
  }

  /// Constructs a quaternion from euler angles (pitch, yaw, roll).
  ///
  /// @param xRot The pitch angle in degrees.
  /// @param yRot   The yaw angle in degrees.
  /// @param roll  The roll angle in degrees.
  public static Quaternionf eulerDegToQuat(
      float xRot, float yRot, float roll, @NonNull Quaternionf dest) {
    float pitchRad = xRot * PITCH_SIGN * DEG_TO_RAD;
    float yawRad = YAW_OFFSET_RAD + yRot * YAW_SIGN * DEG_TO_RAD;
    float rollRad = roll * DEG_TO_RAD;
    return dest.rotationYXZ(yawRad, pitchRad, rollRad);
  }

  /// Constructs a quaternion from euler angles `(pitch, yaw)` with roll = 0.
  ///
  /// @param eulerDeg The orientation as (pitch, yaw) in degrees.
  public static Quaternionf eulerDegToQuat(@NonNull Vector2fc eulerDeg, @NonNull Quaternionf dest) {
    return eulerDegToQuat(eulerDeg.x(), eulerDeg.y(), dest);
  }

  /// Constructs a quaternion from euler angles `(pitch, yaw, roll)`.
  ///
  /// @param eulerDeg The orientation as (pitch, yaw, roll) in degrees.
  public static Quaternionf eulerDegToQuat(@NonNull Vector3fc eulerDeg, @NonNull Quaternionf dest) {
    return eulerDegToQuat(eulerDeg.x(), eulerDeg.y(), eulerDeg.z(), dest);
  }

  /// Constructs a quaternion from Minecraft's native orientation angles `(pitch=x, yaw=y)`.
  ///
  /// @param eulerDeg The orientation as (pitch, yaw) in degrees.
  public static Quaternionf eulerDegToQuat(@NonNull Vec2 eulerDeg, @NonNull Quaternionf dest) {
    return eulerDegToQuat(eulerDeg.x, eulerDeg.y, dest);
  }

  /// Extracts (pitch, yaw) from a quaternion, discarding roll.
  ///
  /// @return `dest` set to (pitch, yaw) in degrees.
  public static Vector2f quatToEulerDeg(@NonNull Quaternionfc rotation, @NonNull Vector2f dest) {
    Vector3f full = quatToEulerDeg(rotation, new Vector3f());
    return dest.set(full.x(), full.y());
  }

  /// Extracts (pitch, yaw, roll) from a quaternion.
  ///
  /// @return `dest` set to (pitch, yaw, roll) in degrees.
  public static Vector3f quatToEulerDeg(@NonNull Quaternionfc rotation, @NonNull Vector3f dest) {
    final Vector3f eulerRad = new Vector3f();
    rotation.getEulerAnglesYXZ(eulerRad);

    return dest.set(
        eulerRad.x() * RAD_TO_DEG * PITCH_SIGN,
        (eulerRad.y() - YAW_OFFSET_RAD) * RAD_TO_DEG * YAW_SIGN,
        eulerRad.z() * RAD_TO_DEG);
  }

  // endregion

  // region euler utilities

  /// Normalizes euler angles so that `pitch` ∈ `[-90, 90]` when possible and `yaw/roll` ∈ `(-180,
  /// 180]`.
  ///
  /// Useful for stable comparisons and interpolation endpoints.
  ///
  /// @param eulerDeg The orientation as (pitch, yaw, roll) in degrees (modified in-place).
  /// @return the same `eulerDeg` instance for chaining.
  public static Vector3f normalizeEulerDeg(@NonNull Vector3f eulerDeg) {
    float pitch = eulerDeg.x();
    float yaw = eulerDeg.y();
    float roll = eulerDeg.z();

    // Wrap yaw and roll into (-180, 180]
    yaw = wrapAngle(yaw);
    roll = wrapAngle(roll);

    // Normalize pitch: if outside [-90, 90], flip and compensate yaw/roll
    pitch = wrapAngle(pitch);
    if (pitch > 90.0f) {
      pitch = 180.0f - pitch;
      yaw = wrapAngle(yaw + 180.0f);
      roll = wrapAngle(roll + 180.0f);
    } else if (pitch < -90.0f) {
      pitch = -180.0f - pitch;
      yaw = wrapAngle(yaw + 180.0f);
      roll = wrapAngle(roll + 180.0f);
    }

    return eulerDeg.set(pitch, yaw, roll);
  }

  /// Wraps an angle in degrees into the range `(-180, 180]`.
  public static float wrapAngle(float degrees) {
    float d = degrees % 360.0f;
    if (d > 180.0f) d -= 360.0f;
    else if (d <= -180.0f) d += 360.0f;
    return d;
  }

  // endregion
}
