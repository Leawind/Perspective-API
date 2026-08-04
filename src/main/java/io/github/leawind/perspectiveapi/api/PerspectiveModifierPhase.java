package io.github.leawind.perspectiveapi.api;

/// The stage at which a {@link PerspectiveModifier} is applied.
public enum PerspectiveModifierPhase {
  /// Applies to the perspective's target state before perspective-switch transition interpolation.
  ///
  /// Use this phase when the modified result should participate in the transition.
  BEFORE_TRANSITION,

  /// Applies to the final visual state after perspective-switch transition interpolation.
  ///
  /// Use this phase for effects such as camera shake or roll that should remain at full strength
  /// while a perspective transition is in progress.
  AFTER_TRANSITION;
}
