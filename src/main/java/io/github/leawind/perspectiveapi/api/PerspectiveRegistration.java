package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Owns one runtime perspective registration.
///
/// A handle can only update or remove the exact registration that created it. Reusing a
/// perspective ID later does not allow an old handle to affect the new registration.
@ApiStatus.NonExtendable
public interface PerspectiveRegistration {
  /// Returns the registered perspective.
  @NonNull Perspective perspective();

  /// Returns whether this registration is still present in its registry.
  boolean isRegistered();

  /// Updates runtime information without changing the identity of the registered perspective.
  ///
  /// The new information must retain the original perspective ID.
  ///
  /// @throws IllegalArgumentException if the perspective ID changes
  /// @throws IllegalStateException if this registration has already been removed
  @ApiStatus.Experimental
  void updateInfo(@NonNull PerspectiveInfo info);

  /// Removes this exact registration.
  ///
  /// If the perspective is active, selection and lifecycle state are reconciled on the next active
  /// client tick. The behavior must remain usable until its `onDeactivate()` callback.
  ///
  /// @return `true` if the registration was removed, or `false` if it was already absent
  /// @throws IllegalStateException if removing it would leave the registry without a default
  ///     perspective
  boolean unregister();
}
