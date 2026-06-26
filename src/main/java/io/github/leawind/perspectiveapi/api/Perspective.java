package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveRenderTickContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

  /// Returns the camera state this perspective wants to apply.
  ///
  /// Called every render tick after {@link #renderTick} has been invoked to compute the state.
  ///
  /// @return the state, or `null` to keep vanilla defaults.
  default @Nullable PerspectiveState getState() {
    return null;
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
  /// @see #clientTick
  default void renderTick(@NonNull PerspectiveRenderTickContext context) {}

  // endregion
}
