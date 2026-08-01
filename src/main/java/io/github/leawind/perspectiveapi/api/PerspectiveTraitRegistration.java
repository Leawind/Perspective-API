package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;

/// Owns one external trait contribution for a perspective ID.
///
/// A handle can only inspect or remove the exact contribution that created it. Removing it does not
/// affect traits declared by the perspective or contributed through another registration.
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface PerspectiveTraitRegistration {
  /// Returns whether this contribution is still registered.
  boolean isRegistered();

  /// Removes this exact contribution.
  ///
  /// @return `true` if the contribution was removed, or `false` if it was already absent
  boolean unregister();
}
