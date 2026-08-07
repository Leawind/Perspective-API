package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// A read-only view of a registered perspective.
///
/// Each instance is backed by a {@link PerspectiveBehavior} and carries metadata from either a
/// {@link PerspectiveInfo.Declaration} annotation or a runtime {@link PerspectiveInfo}.
///
/// Perspectives are created by the {@link PerspectiveRegistry} and should not be implemented
/// directly.
///
/// @see PerspectiveBehavior
/// @see PerspectiveInfo.Declaration
/// @see PerspectiveInfo
@ApiStatus.NonExtendable
public interface Perspective {
  /// Returns the metadata specified when this perspective was registered.
  @NonNull PerspectiveInfo info();

  /// Returns whether this perspective is currently eligible to be resolved as active.
  ///
  /// The underlying {@link PerspectiveBehavior#isAvailable()} is evaluated independently for every
  /// call. Perspective API does not cache availability. Implementations should return a cheap,
  /// side-effect-free cached value when their availability depends on state updated elsewhere.
  ///
  /// If no available candidate can be resolved, the default perspective is used as a safety
  /// fallback even if it reports itself as unavailable.
  ///
  /// @see PerspectiveBehavior#isAvailable()
  boolean isAvailable();
}
