package io.github.leawind.perspectiveapi.api;

import org.jspecify.annotations.NonNull;

/// Manages an ordered chain of {@link PerspectiveModifier}s.
///
/// Modifiers are applied sequentially by ascending priority after the base perspective
/// establishes the target camera state, but before the transition interpolation.
public interface PerspectiveModifierChain {

  /// Registers a modifier with the given priority.
  ///
  /// If an entry with the same key already exists, it is replaced.
  /// Lower priority values are applied first.
  void register(@NonNull String key, int priority, @NonNull PerspectiveModifier modifier);

  /// Removes the modifier entry with the given key.
  void unregister(@NonNull String key);
}
