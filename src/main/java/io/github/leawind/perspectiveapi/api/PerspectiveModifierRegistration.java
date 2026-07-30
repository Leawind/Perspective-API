package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;

/// Owns one entry in a {@link PerspectiveModifierChain}.
///
/// A handle can only inspect or remove the exact registration that created it.
@ApiStatus.NonExtendable
public interface PerspectiveModifierRegistration {
  /// Returns whether this registration is still present in its chain.
  boolean isRegistered();

  /// Removes this exact registration.
  ///
  /// @return `true` if the registration was removed, or `false` if it was already absent
  boolean unregister();
}
