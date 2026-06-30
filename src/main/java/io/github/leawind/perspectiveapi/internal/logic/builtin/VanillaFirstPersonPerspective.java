package io.github.leawind.perspectiveapi.internal.logic.builtin;

import net.minecraft.client.CameraType;

/// Built-in first-person perspective matching vanilla Minecraft behavior.
///
/// Since the mixin intercepts after vanilla Camera.setup, the camera is already at the correct
/// first-person position. This perspective does nothing, preserving vanilla state.
public final class VanillaFirstPersonPerspective extends VanillaPerspective {
  /// Singleton instance of the first-person perspective.
  public static final VanillaFirstPersonPerspective INSTANCE = new VanillaFirstPersonPerspective();

  private VanillaFirstPersonPerspective() {
    super("first_person", CameraType.FIRST_PERSON);
  }
}
