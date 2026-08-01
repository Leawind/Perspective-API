package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;

/// Owns one entry in a {@link PerspectiveModifierChain}.
///
/// A handle can only remove the exact registration that created it.
@ApiStatus.NonExtendable
public interface PerspectiveModifierRegistration {
  /// Removes this exact registration.
  ///
  /// @return `true` if the registration was removed, or `false` if it was already absent
  boolean unregister();
}
