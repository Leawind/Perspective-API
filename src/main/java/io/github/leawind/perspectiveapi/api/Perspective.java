package io.github.leawind.perspectiveapi.api;

import java.util.Set;
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
  /// Returns the current metadata for this registered perspective.
  ///
  /// The returned value may change after {@link
  /// PerspectiveRegistration#updateInfo(PerspectiveInfo)}, while this registered perspective
  /// retains its identity.
  @NonNull PerspectiveInfo info();

  /// Returns the effective semantic traits of this perspective.
  ///
  /// The returned immutable snapshot is the union of traits declared by {@link #info()} and traits
  /// contributed through {@link PerspectiveRegistry#contributeTraits}. Removing one contribution
  /// does not remove the same trait when another source still provides it.
  @ApiStatus.Experimental
  default @NonNull Set<@NonNull String> traits() {
    return info().traits();
  }

  /// Returns whether this perspective has the given effective semantic trait.
  @ApiStatus.Experimental
  default boolean hasTrait(@NonNull String trait) {
    PerspectiveInfo.validateTrait(trait);
    return traits().contains(trait);
  }

  /// Returns whether this perspective is currently eligible to be resolved as active.
  ///
  /// The underlying {@link PerspectiveBehavior#isAvailable()} is evaluated lazily at most once per
  /// active client tick. Repeated calls during the same tick reuse the same result.
  ///
  /// If no available candidate can be resolved, the default perspective is used as a safety
  /// fallback even if it reports itself as unavailable.
  boolean isAvailable();
}
