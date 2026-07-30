package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Manages an ordered chain of {@link PerspectiveModifier}s.
///
/// Modifiers are applied sequentially by ascending priority after the base perspective
/// establishes the target camera state, but before the transition interpolation. Entries with the
/// same priority are applied in registration order.
@ApiStatus.NonExtendable
public interface PerspectiveModifierChain {

  /// Registers a modifier with the given priority and returns a handle that owns the registration.
  ///
  /// Lower priority values are applied first.
  ///
  /// @param priority the application priority
  /// @param modifier the modifier to apply
  @NonNull PerspectiveModifierRegistration register(
      int priority, @NonNull PerspectiveModifier modifier);
}
