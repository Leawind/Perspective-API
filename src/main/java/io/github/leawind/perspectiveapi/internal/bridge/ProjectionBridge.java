package io.github.leawind.perspectiveapi.internal.bridge;

/*? if >=26.1 {*/
import com.mojang.blaze3d.systems.RenderSystem;
/*? }*/
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/// Cross-version helpers for constructing world projection matrices.
public final class ProjectionBridge {
  private ProjectionBridge() {}

  /// Returns whether a world frustum matrix uses an orthographic projection.
  ///
  /// The camera view matrix is affine, so its product with the projection matrix remains affine
  /// exactly when the projection is orthographic.
  public static boolean isOrthographicFrustumMatrix(Matrix4fc matrix) {
    return matrix.isAffine();
  }

  /// Replaces `dest` with an orthographic projection centered on the camera's forward axis.
  public static Matrix4f setCenteredOrthographic(
      Matrix4f dest,
      float orthographicHeight,
      float aspectRatio,
      float zNear,
      float zFar,
      boolean reverseDepth) {
    float halfHeight = orthographicHeight * 0.5f;
    float halfWidth = halfHeight * aspectRatio;
    /*? if >=26.2 {*/
    boolean useReverseDepth = reverseDepth;
    /*? } else {*/
    /*boolean useReverseDepth = false;
    *//*? }*/
    float near = useReverseDepth ? zFar : zNear;
    float far = useReverseDepth ? zNear : zFar;

    /*? if >=26.2 {*/
    return dest.setOrtho(
        -halfWidth,
        halfWidth,
        -halfHeight,
        halfHeight,
        near,
        far,
        RenderSystem.getDevice().getDeviceInfo().isZZeroToOne());
    /*? } else if >=26.1 {*/
    /*return dest.setOrtho(
        -halfWidth,
        halfWidth,
        -halfHeight,
        halfHeight,
        near,
        far,
        RenderSystem.getDevice().isZZeroToOne());
    *//*? } else {*/
    /*return dest.setOrtho(-halfWidth, halfWidth, -halfHeight, halfHeight, near, far);
    *//*? }*/
  }
}
