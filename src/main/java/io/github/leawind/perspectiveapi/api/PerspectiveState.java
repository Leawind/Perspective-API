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
/// and projection settings.
@ApiStatus.NonExtendable
public interface PerspectiveState {

  /// Returns the camera position in world space.
  @NonNull Vector3dc position();

  /// Returns the camera rotation (API convention, +Z forward).
  @NonNull Quaternionfc rotation();

  /// Returns the field of view in degrees.
  ///
  /// This value is effective only when {@link #projectionMode()} is
  /// {@link ProjectionMode#PERSPECTIVE}.
  float getFovDeg();

  /// Returns the projection mode used to render the world.
  @ApiStatus.Experimental
  @NonNull ProjectionMode projectionMode();

  /// Returns the vertical span of the orthographic view in world units.
  ///
  /// The horizontal span is this value multiplied by the viewport aspect ratio. This value is
  /// effective only when {@link #projectionMode()} is {@link ProjectionMode#ORTHOGRAPHIC}.
  @ApiStatus.Experimental
  float getOrthographicHeight();

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

    /// Sets the projection mode used to render the world.
    ///
    /// @throws NullPointerException if `projectionMode` is `null`
    @ApiStatus.Experimental
    void setProjectionMode(@NonNull ProjectionMode projectionMode);

    /// Sets the vertical span of the orthographic view in world units.
    @ApiStatus.Experimental
    void setOrthographicHeight(float orthographicHeight);
  }
}
