package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

public interface Perspective {
  // region meta info

  /// Returns the unique identifier of this perspective.
  ///
  /// Recommended format: `<modid>:<path>`
  ///
  /// Example: `examplemod:free_camera`
  @NonNull Identifier id();

  /// Corresponding camera type for this perspective.
  @NonNull CameraType cameraType();

  // endregion

  /// Whether camera transition is enabled when switching to this perspective.
  ///
  /// Checked on every render tick.
  default boolean allowTransition() {
    return true;
  }

  /// Returns whether this perspective is currently available.
  ///
  /// Checked on every client tick.
  default boolean isAvailable() {
    return true;
  }

  /// Modifies the vanilla camera's spatial state in-place.
  ///
  /// Called after {@link #renderTick}
  ///
  /// The `position` and `rotation` parameters represent the current vanilla camera
  /// state. This method should mutate them to apply the desired perspective transformation. If this
  /// method does nothing, the vanilla state is preserved as-is.
  ///
  /// @param ctx      The context containing frame-specific data.
  /// @param position The vanilla camera position in world space. Can be mutated.
  /// @param rotation The vanilla camera rotation. Can be mutated.
  default void applyTransform(
      @NonNull PerspectiveContext ctx, @NonNull Vector3d position, @NonNull Quaternionf rotation) {}

  /// Calculates the final Field of View based on the vanilla FOV.
  /// 
  /// Called after {@link #renderTick}
  ///
  /// @param ctx The context containing frame-specific data.
  /// @param vanillaFovDeg The vanilla camera FOV in degrees.
  /// @return The final FOV to be applied, in degrees.
  default float applyFov(@NonNull PerspectiveContext ctx, float vanillaFovDeg) {
    return vanillaFovDeg;
  }

  // region events

  /// Called when this perspective becomes the current perspective (The one got from {@link
  /// PerspectiveManager#getCurrent()}).
  ///
  /// @see #onDeactivate()
  default void onActivate() {}

  /// Called when this perspective is no longer the current perspective (The one got from {@link
  /// PerspectiveManager#getCurrent()}).
  ///
  /// @see #onActivate()
  default void onDeactivate() {}

  /// Called every client tick while this perspective is active.
  ///
  /// @see #renderTick
  default void clientTick(@NonNull Minecraft minecraft) {}

  /// Called on render tick while this perspective is active.
  ///
  /// Before {@link #applyTransform} and {@link #applyFov}
  ///
  /// @see #clientTick
  default void renderTick(@NonNull PerspectiveContext context) {}

  // endregion
}
