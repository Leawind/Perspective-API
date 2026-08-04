package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Manages an ordered chain of {@link PerspectiveModifier}s.
///
/// Modifiers are applied sequentially by ascending priority before perspective-switch transition
/// interpolation. Entries with the same priority are applied in registration order. Different
/// mods should not rely on registration order to coordinate modifiers with the same priority.
@ApiStatus.NonExtendable
public interface PerspectiveModifierChain {

  /// Registers a modifier and returns a handle that owns the registration.
  ///
  /// Modifier IDs are global, stable diagnostic identifiers. Lower priority values are applied
  /// first.
  ///
  /// @param id the non-empty stable modifier ID
  /// @param priority the application priority
  /// @param modifier the modifier to apply
  /// @throws IllegalArgumentException if `id` is empty or already registered
  @NonNull PerspectiveModifierRegistration register(
      @NonNull String id, int priority, @NonNull PerspectiveModifier modifier);
}
