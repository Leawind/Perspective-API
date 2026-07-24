package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

/// A read-only, temporary view of the camera state managed by Perspective API.
///
/// Comprises position (world space), rotation (API convention, +Z forward),
/// and field of view in degrees.
@ApiStatus.NonExtendable
public interface PerspectiveState {

  /// Returns the camera position in world space.
  @NonNull Vector3dc position();

  /// Returns the camera rotation (API convention, +Z forward).
  @NonNull Quaternionfc rotation();

  /// Returns the field of view in degrees.
  float getFovDeg();

  /// A mutable view of {@link PerspectiveState} whose spatial fields can be
  /// modified in-place.
  ///
  /// Passed to {@link PerspectiveBehavior#applyCameraState} and
  /// {@link PerspectiveModifier#apply} during the camera state computation pipeline.
  @ApiStatus.NonExtendable
  interface Mutable extends PerspectiveState {

    @Override
    @NonNull Vector3d position();

    @Override
    @NonNull Quaternionf rotation();

    /// Sets the field of view in degrees.
    void setFovDeg(float fovDeg);
  }
}
