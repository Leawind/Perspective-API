package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveRenderTickContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

public interface Perspective {
  // region meta info

  /// Returns the unique identifier of this perspective.
  @NonNull Identifier id();

  @NonNull CameraType cameraType();

  /// Determines whether it should override the vanilla camera logic for this perspective.
  ///
  /// Returning `false` disables all camera modifications provided by {@link PerspectiveAPI},
  /// allowing your mod to apply its own camera modifications through Mixin or something.
  /// Just remember to check if the current perspective is which you are working on.
  ///
  /// ```java
  /// if (PerspectiveManager.get().getCurrentPerspective() != ExamplePerspective.INSTANCE) return;
  /// ```
  default boolean shouldOverrideVanillaCamera() {
    return true;
  }

  /// Whether camera transition is enabled when switching to this perspective.
  default boolean allowTransition() {
    return true;
  }

  // endregion

  // region dynamic

  /// Returns the camera position in world coordinates when this perspective is active.
  @NonNull Vector3dc getPosition();

  /// Returns the rotation of the camera when this perspective is active.
  @NonNull Quaternionfc getRotation();

  /// Modifies the field of view. Return the modified value, or `vanillaFieldOfView` to keep
  /// vanilla behavior.
  default float getFieldOfView(float vanillaFieldOfView) {
    return vanillaFieldOfView;
  }

  // endregion

  // region events

  /// Returns whether this perspective is currently available.
  ///
  /// Checked on every client tick.
  default boolean isAvailable() {
    return true;
  }

  /// Called when this perspective becomes the active perspective.
  default void onActivate() {}

  /// Called every client tick while this perspective is active.
  default void clientTick(Minecraft minecraft) {}

  /// Called on render tick while this perspective is active.
  default void renderTick(@NonNull PerspectiveRenderTickContext context) {}

  default void onDeactivate() {}

  // endregion
}
