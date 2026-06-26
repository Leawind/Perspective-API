package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveRenderTickContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;
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

  /// Whether camera transition is enabled when switching to this perspective.
  ///
  /// Checked on every render tick.
  default boolean allowTransition() {
    return true;
  }

  // endregion

  // region dynamic

  /// Returns whether this perspective is currently available.
  ///
  /// Checked on every client tick.
  default boolean isAvailable() {
    return true;
  }

  /// Returns the camera position in world coordinates when this perspective is active.
  ///
  /// Called every render tick.
  ///
  /// @return the position, or `null` to keep original.
  default @Nullable Vector3dc getPosition() {
    return null;
  }

  /// Returns the rotation of the camera when this perspective is active.
  ///
  /// Called every render tick.
  /// @return the rotation, or `null` to keep original.
  default @Nullable Quaternionfc getRotation() {
    return null;
  }

  /// Modifies the field of view. Return the modified value, or `null` to keep
  /// original behavior.
  ///
  /// Called every render tick.
  ///
  /// @return the field of view, or `null` to keep original.
  default @Nullable Float getFieldOfView() {
    return null;
  }

  // endregion

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
