package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

/// A read-only view or snapshot of camera state managed by Perspective API.
///
/// Comprises position (world space), rotation (API convention, +Z forward),
/// and projection settings.
/// The method or callback providing a state documents whether it is temporary or can be retained.
@ApiStatus.NonExtendable
public interface PerspectiveState {

  /// Returns the camera position in world space.
  @NonNull Vector3dc position();

  /// Returns the camera rotation (API convention, +Z forward).
  ///
  /// The quaternion is always finite and has unit length.
  @NonNull Quaternionfc rotation();

  /// Returns the projection mode used to render the world.
  ///
  /// @see #getFovDeg()
  /// @see #getOrthographicHeight()
  @ApiStatus.Experimental
  @NonNull ProjectionMode projectionMode();

  /// Returns the field of view in degrees.
  ///
  /// This value is effective only when {@link #projectionMode()} is
  /// {@link ProjectionMode#PERSPECTIVE}.
  /// During camera-state calculation, its initial value is the latest valid vanilla FOV captured
  /// on an earlier render frame. Each valid capture is used by the next camera-state calculation;
  /// invalid samples are ignored. This intentional one-frame cache keeps the state pipeline
  /// consistent across supported Minecraft versions.
  /// It is always finite and in the open range `(0, 180)`.
  float getFovDeg();

  /// Returns the vertical span of the orthographic view in world units.
  ///
  /// The horizontal span is this value multiplied by the viewport aspect ratio. This value is
  /// effective only when {@link #projectionMode()} is {@link ProjectionMode#ORTHOGRAPHIC}.
  /// It is always finite and at least `0.0001`. There is no project-defined upper bound.
  @ApiStatus.Experimental
  float getOrthographicHeight();

  /// A mutable view of {@link PerspectiveState} whose spatial fields can be
  /// modified in-place.
  ///
  /// Passed to {@link PerspectiveBehavior#applyCameraState} and
  /// {@link PerspectiveModifier#apply} during camera state computation.
  @ApiStatus.NonExtendable
  interface Mutable extends PerspectiveState {

    @Override
    @NonNull Vector3d position();

    /// Returns the mutable camera rotation.
    ///
    /// The value must remain finite and have unit length. An invalid value is rejected by the
    /// camera pipeline and replaced with the state from before the current perspective or modifier
    /// callback.
    @Override
    @NonNull Quaternionf rotation();

    /// Sets the projection mode used to render the world.
    ///
    /// @throws NullPointerException if `projectionMode` is `null`
    /// @see #setFovDeg(float)
    /// @see #setOrthographicHeight(float)
    @ApiStatus.Experimental
    void setProjectionMode(@NonNull ProjectionMode projectionMode);

    /// Sets the field of view in degrees.
    ///
    /// The value must be finite and in the open range `(0, 180)`. An invalid value is rejected by
    /// the camera pipeline and replaced with the state from before the current perspective or
    /// modifier callback.
    void setFovDeg(float fovDeg);

    /// Sets the vertical span of the orthographic view in world units.
    ///
    /// The value must be finite and at least `0.0001`; it has no project-defined upper bound. An
    /// invalid value is rejected by the camera pipeline and replaced with the state from before the
    /// current perspective or modifier callback.
    @ApiStatus.Experimental
    void setOrthographicHeight(float orthographicHeight);
  }
}
