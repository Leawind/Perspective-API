package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;

/// Owns one {@link PerspectiveChangeListener} subscription.
///
/// A handle can only remove the exact registration that created it.
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface PerspectiveChangeListenerRegistration {

  /// Removes this exact listener registration.
  ///
  /// @return `true` if the registration was removed, or `false` if it was already absent
  boolean unregister();
}
