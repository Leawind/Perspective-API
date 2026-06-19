package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.PerspectiveAPI;
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

  default boolean allowTransition() {
    return true;
  }

  // endregion

  // region dynamic

  @NonNull Vector3dc getPosition();

  /// Returns the rotation of the camera when this perspective is active.
  @NonNull Quaternionfc getRotation();

  default float getFieldOfView(float vanillaFieldOfView) {
    return vanillaFieldOfView;
  }

  // endregion

  // region events

  /// Returns whether this perspective is currently available.
  default boolean isAvailable() {
    return true;
  }

  default void onActivate() {}

  default void clientTick(Minecraft minecraft) {}

  /// Called on render tick while this perspective is active.
  default void renderTick(@NonNull PerspectiveRenderTickContext context) {}

  default void onDeactivate() {}

  // endregion
}
