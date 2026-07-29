package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;

/// The projection mode used to render the world.
///
/// This type is independent of Minecraft's renderer classes so it remains stable across
/// game versions.
@ApiStatus.Experimental
public enum ProjectionMode {
  /// The vanilla perspective projection.
  PERSPECTIVE,

  /// An orthographic projection centered on the camera's forward axis.
  ORTHOGRAPHIC;
}
