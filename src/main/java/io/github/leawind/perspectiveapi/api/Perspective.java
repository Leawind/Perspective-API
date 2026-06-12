package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.context.PerspectiveTickContext;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

public interface Perspective {
  // region meta info

  /// Returns the unique identifier of this perspective.
  @NonNull Identifier id();

  @NonNull CameraType cameraType();

  @Deprecated(since = "1.0.0")
  default boolean shouldOverrideVanillaCamera() {
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

  /// Called on render tick while this perspective is active.
  default void tick(PerspectiveTickContext context) {}

  default void onActivate() {}

  default void onDeactivate() {}

  // endregion
}
