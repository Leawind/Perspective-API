package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Owns one runtime perspective registration.
///
/// A handle can only remove the exact registration that created it. Reusing a perspective ID later
/// does not allow an old handle to affect the new registration.
@ApiStatus.NonExtendable
public interface PerspectiveRegistration {
  /// Returns the registered perspective.
  @NonNull Perspective perspective();

  /// Removes this exact registration.
  ///
  /// If the perspective is active, selection and lifecycle state are reconciled before the next
  /// main-camera render update. The behavior must remain usable until its `onDeactivate()`
  /// callback.
  ///
  /// @return `true` if the registration was removed, or `false` if it was already absent
  /// @throws IllegalStateException if removing it would leave the registry without a default
  ///   perspective
  boolean unregister();
}
