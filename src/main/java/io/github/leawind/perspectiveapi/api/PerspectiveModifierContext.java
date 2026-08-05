package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Context provided to a {@link PerspectiveModifier} during camera-state computation.
///
/// @apiNote This object is valid only for the duration of the callback in which it is received. It
///   must not be stored or referenced after that callback returns.
@ApiStatus.NonExtendable
public interface PerspectiveModifierContext extends PerspectiveContext {

  /// Returns the resolved state produced by the active perspective for this frame.
  ///
  /// This is an independent, read-only snapshot captured after the perspective callback and its
  /// validation complete, but before any modifier or perspective-switch transition runs. It does
  /// not change when earlier modifiers mutate their current state.
  ///
  /// @apiNote This snapshot is temporary and must not be retained after the modifier callback.
  @NonNull PerspectiveState perspectiveBaseState();
}
