package io.github.leawind.perspectiveapi.internal.bridge;

import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("ConstantConditions")
public final class CameraSpace {
  static final boolean IS_INTERNAL_IDENTITY_QUAT_NEGATIVE_Z;
  private static final float EULER_SINGULARITY_EPSILON = 1.0e-6f;

  // region const fields
  public static final Vector3fc UP;
  public static final Vector3fc DOWN;
  public static final Vector3fc FORWARD;
  public static final Vector3fc BACKWARD;
  public static final Vector3fc LEFT;
  public static final Vector3fc RIGHT;

  private static final float PITCH_SIGN;
  private static final float YAW_SIGN;
  private static final float YAW_OFFSET_RAD;

  static {
    /*? if >=1.21 {*/
    IS_INTERNAL_IDENTITY_QUAT_NEGATIVE_Z = true;
    /*? } else {*/
    /*IS_INTERNAL_IDENTITY_QUAT_NEGATIVE_Z = false;
     */
    /*? }*/

    UP = new Vector3f(0, 1, 0);
    DOWN = new Vector3f(0, -1, 0);

    if (IS_INTERNAL_IDENTITY_QUAT_NEGATIVE_Z) {
      FORWARD = new Vector3f(0, 0, -1);
      BACKWARD = new Vector3f(0, 0, 1);
      LEFT = new Vector3f(-1, 0, 0);
      RIGHT = new Vector3f(1, 0, 0);
      PITCH_SIGN = -1;
      YAW_SIGN = -1;
      YAW_OFFSET_RAD = (float) Math.PI;
    } else {
      FORWARD = new Vector3f(0, 0, 1);
      BACKWARD = new Vector3f(0, 0, -1);
      LEFT = new Vector3f(1, 0, 0);
      RIGHT = new Vector3f(-1, 0, 0);
      PITCH_SIGN = 1;
      YAW_SIGN = -1;
      YAW_OFFSET_RAD = 0;
    }
  }

  public static Quaternionf apiToMc(Quaternionfc apiQuat, Quaternionf dest) {
    if (IS_INTERNAL_IDENTITY_QUAT_NEGATIVE_Z) {
      return apiQuat.rotateY((float) Math.PI, dest);
    } else {
      return dest.set(apiQuat);
    }
  }

  public static Quaternionf mcToApi(Quaternionfc mcQuat, Quaternionf dest) {
    return apiToMc(mcQuat, dest);
  }

  public static Quaternionf eulerDegToMcQuat(
      float pitchDeg, float yawDeg, float rollDeg, @NonNull Quaternionf dest) {
    float pitchRad = Math.toRadians(pitchDeg * PITCH_SIGN);
    float yawRad = YAW_OFFSET_RAD + Math.toRadians(yawDeg * YAW_SIGN);
    float rollRad = Math.toRadians(rollDeg);
    return dest.rotationYXZ(yawRad, pitchRad, rollRad);
  }

  public static Vector3f mcQuatToEulerDeg(@NonNull Quaternionfc mcQuat, @NonNull Vector3f dest) {
    final Vector3f eulerRad = getEulerAnglesYXZ(mcQuat, new Vector3f());
    return dest.set(
        Math.toDegrees(eulerRad.x()) * PITCH_SIGN,
        Math.toDegrees(eulerRad.y() - YAW_OFFSET_RAD) * YAW_SIGN,
        Math.toDegrees(eulerRad.z()));
  }

  public static Vector2f mcQuatToEulerDeg(Quaternionfc mcQuat, Vector2f dest) {
    final Vector3f eulerRad = getEulerAnglesYXZ(mcQuat, new Vector3f());
    return dest.set(
        Math.toDegrees(eulerRad.x()) * PITCH_SIGN,
        Math.toDegrees(eulerRad.y() - YAW_OFFSET_RAD) * YAW_SIGN);
  }

  private static Vector3f getEulerAnglesYXZ(Quaternionfc rotation, Vector3f dest) {
    float x = rotation.x();
    float y = rotation.y();
    float z = rotation.z();
    float w = rotation.w();
    float lengthSquared = rotation.lengthSquared();
    if (!(lengthSquared > 0.0f) || !Float.isFinite(lengthSquared)) {
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

  private CameraSpace() {}
}
