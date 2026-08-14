package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Represents a camera perspective that can be applied to the game camera.
///
/// A Perspective establishes the base camera state before cooperative {@link PerspectiveModifier}
/// transformations are applied.
///
/// PerspectiveBehavior implementations can be discovered via {@link java.util.ServiceLoader} or
/// registered at runtime through the {@link PerspectiveRegistry}.
///
/// Service-loaded implementations must be annotated with {@link PerspectiveInfo.Declaration}.
/// Runtime registrations provide equivalent metadata through {@link PerspectiveInfo} and do not
/// require an annotation.
///
/// @see Perspective
/// @see PerspectiveInfo
/// @see PerspectiveRegistry#register
/// @see PerspectiveRegistry#registerDefault
@ApiStatus.OverrideOnly
public interface PerspectiveBehavior {
  /// Values that map one-to-one to Minecraft's vanilla camera types.
  ///
  /// A base type selects vanilla behavior that depends on the current camera type before this
  /// perspective produces its camera state. Those behaviors are version-dependent and opaque to
  /// Perspective API. Callers must not infer finer semantics from behavior observed in one
  /// Minecraft version.
  enum BaseType {
    FIRST_PERSON,
    THIRD_PERSON_BACK,
    THIRD_PERSON_FRONT;
  }

  /// Whether smooth transitions are allowed when switching TO this perspective.
  ///
  /// This method is evaluated once when a switch to this perspective occurs. A failure is logged
  /// and treated as `false`.
  default boolean allowsTransitionIn() {
    return true;
  }

  /// Whether smooth transitions are allowed when switching FROM this perspective.
  ///
  /// This method is evaluated once when a switch from this perspective occurs. A failure is logged
  /// and treated as `false`.
  default boolean allowsTransitionOut() {
    return true;
  }

  /// Returns whether this perspective is currently eligible to be resolved as active.
  ///
  /// Player selection and {@link PerspectiveOverrideChain} resolution skip unavailable
  /// perspectives. If no available candidate can be resolved, the default perspective is used as a
  /// safety fallback even if it reports itself as unavailable.
  ///
  /// Perspective API evaluates this method whenever availability is needed. It does not cache the
  /// result.
  ///
  /// @return `true` if this perspective is eligible for resolution
  /// @apiNote
  ///   - Implementations must return a cheap cached value here.
  ///   - Do not rely on invocation count or side effects.
  ///   - A failure is logged and treated as `false` for that evaluation.
  default boolean isAvailable() {
    return true;
  }

  /// Called once when the behavior is registered and initialized.
  ///
  /// If this method fails during runtime registration, the registration is rolled back and the
  /// failure is propagated. A failed service-loaded provider is skipped and its failure is logged.
  default void initialize() {}

  // region events

  /// Called when this perspective becomes the current perspective. A failure is logged and
  /// otherwise ignored.
  ///
  /// @see #onDeactivate()
  default void onActivate() {}

  /// Called when this perspective is no longer the current perspective. A failure is logged and
  /// otherwise ignored.
  ///
  /// @see #onActivate()
  default void onDeactivate() {}

  // endregion

  // region camera state pipeline

  /// Modifies the camera's target state in-place.
  ///
  /// As the base perspective, this method receives the vanilla camera state and establishes the
  /// foundational target state. Subsequent {@link PerspectiveModifier}s may mutate this state
  /// before perspective-switch transition interpolation. If this method fails, the failure is
  /// logged and the complete target state is restored to the vanilla state received before this
  /// method was called.
  ///
  /// @param state The vanilla camera state. Can be mutated.
  /// @param context The context containing frame-specific data.
  /// @apiNote `state` must not be stored or referenced outside this method call. `context` is also
  ///   valid only for this call. Repeated rotation calculations can accumulate floating-point
  ///   error, and the camera pipeline rejects rotations outside its unit-length tolerance.
  default void computeCameraState(
      PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext context) {}

  /// Called on every render frame when this perspective is active, after the final camera state has
  /// been written to the Minecraft camera, including all modifier transformations and transition
  /// interpolation.
  ///
  /// Provides the exact position and rotation written to the camera, suitable for raycasts,
  /// hit-testing, or other spatial queries that depend on the actual rendered viewpoint. FOV and
  /// projection settings are applied later in the same frame by the projection pipeline. A failure
  /// is logged and otherwise ignored for that frame.
  ///
  /// @param state The final camera state that has been applied.
  /// @param context The context containing frame-specific data.
  /// @apiNote Neither argument may be stored or referenced after this method returns.
  default void afterCameraStateResolved(
      @NonNull PerspectiveState state, @NonNull PerspectiveContext context) {}

  // endregion
}
