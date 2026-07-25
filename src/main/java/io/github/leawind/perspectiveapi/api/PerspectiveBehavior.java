package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Represents a camera perspective that can be applied to the game camera.
///
/// A Perspective acts as the foundational {@link PerspectiveModifier} that establishes
/// the base camera state.
///
/// PerspectiveBehavior implementations are discovered via {@link java.util.ServiceLoader} and
/// registered by the {@link PerspectiveRegistry}.
///
/// Implementations must be annotated with {@link Info}.
///
/// @see Perspective
@ApiStatus.OverrideOnly
public interface PerspectiveBehavior {
  /// Logical base types for camera perspectives, decoupled from Minecraft's internal
  /// implementation.
  ///
  /// Using this enum instead of `net.minecraft.client.CameraType` prevents `AnnotationFormatError`
  /// during early mod initialization and ensures the API remains stable across game versions.
  enum BaseType {
    FIRST_PERSON,
    THIRD_PERSON_BACK,
    THIRD_PERSON_FRONT;
  }

  /// Annotation that provides metadata for a {@link PerspectiveBehavior}.
  ///
  /// Every implementation must carry this annotation so the registry can
  /// derive a {@link Perspective} from it.
  ///
  /// @see Perspective
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Documented
  @interface Info {
    /// The non-empty identifier for this perspective.
    ///
    /// Recommended format: `<modid>.<path>` (e.g., `examplemod.free_camera`).
    /// If multiple behaviors use the same ID, the behavior with the lower {@link #priority()}
    /// value is registered. Ties are resolved by the lexicographically earlier fully qualified
    /// behavior class name.
    @NonNull String id();

    /// The vanilla camera type used as a fallback when this perspective
    /// does not explicitly modify the camera transform or FOV.
    @NonNull BaseType baseType() default BaseType.THIRD_PERSON_BACK;

    /// The translation key for the perspective's display name.
    ///
    /// If left empty, it defaults to `perspective.<id>.name`.
    ///
    /// @see Perspective#name()
    @NonNull String nameKey() default "";

    /// The translation key for the perspective's description.
    ///
    /// If left empty, it defaults to `null`.
    ///
    /// @see Perspective#description()
    @NonNull String descriptionKey() default "";

    /// The string representation of the `Identifier` or `ResourceLocation` for the
    /// perspective's icon texture.
    ///
    /// If left empty, it defaults to `null`.
    ///
    /// @see Perspective#icon()
    @NonNull String icon() default "";

    /// Whether this perspective is allowed to be manually selected by the player
    /// via a {@link PerspectiveSwitcherBehavior}
    ///
    /// If set to `false`, the perspective can only be activated programmatically
    /// through the {@link PerspectiveOverrideChain}.
    @ApiStatus.Experimental
    boolean switchable() default true;

    /// The sorting priority within the switcher and duplicate-ID resolution.
    ///
    /// Lower values appear earlier in the cycle and take precedence over a duplicate ID.
    /// Switcher ordering is effective only when `switchable` is `true`, but duplicate-ID
    /// resolution always uses this value.
    int priority() default 0;
  }

  /// Marks a perspective behavior as the default one to be activated on startup.
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Documented
  @interface Default {
    /// The priority of this default perspective. Higher values take precedence
    /// if multiple defaults are registered.
    int priority() default 0;
  }

  /// Whether smooth transitions are allowed when switching TO this perspective.
  default boolean allowTransitionIn() {
    return true;
  }

  /// Whether smooth transitions are allowed when switching FROM this perspective.
  default boolean allowTransitionOut() {
    return true;
  }

  /// Returns whether this perspective is currently eligible to be resolved as active.
  ///
  /// Switchers and override resolution skip unavailable perspectives. If no available candidate can
  /// be resolved, the default perspective is used as a safety fallback even if it reports itself as
  /// unavailable.
  ///
  /// @return `true` if this perspective is eligible for resolution
  default boolean isAvailable() {
    return true;
  }

  /// Called once when the behavior is registered and initialized.
  default void init() {}

  // region events

  /// Called when this perspective becomes the current perspective
  ///
  /// @see #onDeactivate()
  default void onActivate() {}

  /// Called when this perspective is no longer the current perspective
  ///
  /// @see #onActivate()
  default void onDeactivate() {}

  /// Called every client tick when this perspective is active.
  ///
  /// @see #isAvailable()
  /// @see #preApplyWhenActive
  default void clientTickWhenActive(@NonNull Minecraft minecraft) {}

  // endregion

  // region camera state pipeline

  /// Called on every render frame when this perspective is active,
  /// **before** {@link #applyCameraState}.
  ///
  /// Use this to prepare per-frame data (e.g. reading input, updating
  /// internal state).
  ///
  /// @see #postApplyWhenActive
  default void preApplyWhenActive(@NonNull PerspectiveContext context) {}

  /// Modifies the camera's target state in-place.
  ///
  /// As the base perspective, this method receives the vanilla camera state and establishes the
  /// foundational target state.
  /// Subsequent {@link PerspectiveModifier}s will further mutate this state before transition
  /// interpolation.
  ///
  /// @param state The vanilla camera state. Can be mutated.
  /// @param ctx   The context containing frame-specific data.
  /// @apiNote `state` must not be stored or referenced outside this method
  ///   call. `ctx` is also valid only for this call.
  default void applyCameraState(
      PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext ctx) {}

  /// Called on every render frame when this perspective is active,
  /// **after** the final camera state has been fully computed and applied,
  /// including all modifier transformations and transition interpolation.
  ///
  /// Provides the exact state written to the Minecraft camera, suitable for
  /// raycasts, hit-testing, or other spatial queries that depend on the
  /// actual rendered viewpoint.
  ///
  /// @param state The final camera state that has been applied.
  /// @param ctx   The context containing frame-specific data.
  /// @apiNote Neither argument may be stored or referenced after this method returns.
  /// @see #preApplyWhenActive
  default void postApplyWhenActive(
      @NonNull PerspectiveState state, @NonNull PerspectiveContext ctx) {}

  // endregion
}
