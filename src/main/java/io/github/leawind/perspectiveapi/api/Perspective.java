package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.compute.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.compute.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/// Represents a camera perspective that can be applied to the game camera.
///
/// A Perspective acts as the foundational {@link PerspectiveModifier} that establishes
/// the base camera state. Additionally, it provides metadata (ID, translation key),
/// lifecycle callbacks, and a {@link CameraType} to instruct the vanilla rendering pipeline.
///
/// Perspective instances are owned by the {@link PerspectiveRegistry}. Implementations should not
/// be stored or referenced directly anywhere else.
public interface Perspective extends PerspectiveModifier {
  // region meta info

  /// Corresponding camera type for this perspective.
  @NonNull CameraType cameraType();

  // endregion

  /// Whether smooth transitions are allowed when switching TO this perspective.
  ///
  /// Checked when this perspective becomes {@link PerspectiveAPI#getCurrentPerspective()}.
  default boolean allowTransitionIn() {
    return true;
  }

  /// Whether smooth transitions are allowed when switching FROM this perspective.
  ///
  /// Checked when this perspective is {@link PerspectiveAPI#getCurrentPerspective()} and is about
  /// to be replaced.
  default boolean allowTransitionOut() {
    return true;
  }

  /// Returns whether this perspective is currently available to remain active.
  ///
  /// If `false`, the manager will automatically cycle away from this perspective,
  /// and it will be skipped by the {@link PerspectiveCycler} and {@link PerspectiveOverrideChain}.
  ///
  /// ### ⚠️ Deadlock Warning
  /// If this method returns a cached value, do NOT update the cache in
  /// {@link #clientTickWhenActive} or {@link #renderTickWhenActive} to make it available again.
  /// Once unavailable, these callbacks stop being called.
  ///
  /// @return `true` if available, `false` to trigger an automatic switch.
  @Override
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
  @Override
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
  @Override
  default float applyFov(@NonNull PerspectiveContext ctx, float vanillaFovDeg) {
    return vanillaFovDeg;
  }

  // region events

  /// Called when this perspective becomes the current perspective (the one obtained from {@link
  /// PerspectiveAPI#getCurrentPerspective()}).
  ///
  /// @see #onDeactivate()
  default void onActivate() {}

  /// Called when this perspective is no longer the current perspective (the one obtained from
  /// {@link PerspectiveAPI#getCurrentPerspective()}).
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
