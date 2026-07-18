package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.joml.Vector3d;
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
    /// The unique identifier for this perspective.
    ///
    /// Recommended format: `<modid>.<path>` (e.g., `examplemod.free_camera`).
    String id();

    /// The vanilla {@link CameraType} used as a fallback when this perspective
    /// does not explicitly modify the camera transform or FOV.
    CameraType cameraType() default CameraType.THIRD_PERSON_BACK;

    /// The translation key for the perspective's display name.
    ///
    /// If left empty, it defaults to `perspective.<id>.name`.
    ///
    /// @see Perspective#name()
    String nameKey() default "";

    /// The translation key for the perspective's description.
    ///
    /// If left empty, it defaults to `null`.
    ///
    /// @see Perspective#description()
    String descriptionKey() default "";

    /// The string representation of the `Identifier` or `ResourceLocation` for the
    /// perspective's icon texture.
    ///
    /// If left empty, it defaults to `null`.
    ///
    /// @see Perspective#icon()
    String icon() default "";

    /// Whether this perspective is allowed to be manually selected by the player
    /// via a {@link PerspectiveSwitcherBehavior}
    ///
    /// If set to `false`, the perspective can only be activated programmatically
    /// through the {@link PerspectiveOverrideChain}.
    boolean switchable() default true;

    /// The sorting priority within the switcher.
    ///
    /// Lower values appear earlier in the cycle.
    /// Only effective when `switchable` is `true`.
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

  /// Returns whether this perspective is currently available to remain active.
  ///
  /// ### ⚠️ Deadlock Warning
  /// If this method returns a cached value, do NOT update the cache in
  /// {@link #clientTickWhenActive} or {@link #renderTickWhenActive} to make it available again.
  /// Once unavailable, these callbacks stop being called.
  ///
  /// @return `true` if available, `false` to trigger an automatic switch.
  default boolean isAvailable() {
    return true;
  }

  /// Modifies the camera's spatial target state in-place.
  ///
  /// As the base perspective, this method receives the vanilla camera state and
  /// establishes the foundational target state. Subsequent {@link PerspectiveModifier}s
  /// will further mutate this state before transition interpolation.
  ///
  /// @param ctx The context containing frame-specific data.
  /// @param position The vanilla camera position in world space. Can be mutated.
  /// @param rotation The vanilla camera rotation. Can be mutated.
  /// @apiNote The arguments `position` and `rotation` must not be stored or referenced outside this
  /// method call.
  default void applyTransform(
      @NonNull PerspectiveContext ctx, @NonNull Vector3d position, @NonNull Quaternionf rotation) {}

  /// Calculates the target Field of View (FOV).
  ///
  /// As the base perspective, this method receives the vanilla FOV and establishes
  /// the foundational target FOV. Subsequent modifiers will further mutate this value.
  ///
  /// @param ctx The context containing frame-specific data.
  /// @param vanillaFovDeg The vanilla camera FOV in degrees.
  /// @return The target FOV to be applied, in degrees.
  default float applyFov(@NonNull PerspectiveContext ctx, float vanillaFovDeg) {
    return vanillaFovDeg;
  }

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
  /// @see #renderTickWhenActive
  default void clientTickWhenActive(@NonNull Minecraft minecraft) {}

  /// Called on every render tick when this perspective is active.
  ///
  /// Called before {@link #applyTransform} and {@link #applyFov}.
  ///
  /// @see #clientTickWhenActive
  default void renderTickWhenActive(@NonNull PerspectiveContext context) {}

  // endregion
}
