package io.github.leawind.perspectiveapi.internal.logic.builtin;

import net.minecraft.client.CameraType;

/// Built-in third-person perspective matching vanilla Minecraft behavior.
public final class VanillaThirdPersonPerspective extends VanillaPerspective {
  /// Third-person back view (camera behind player).
  public static final VanillaThirdPersonPerspective BACK =
      new VanillaThirdPersonPerspective("third_person_back", CameraType.THIRD_PERSON_BACK);
  /// Third-person front view (camera in front of player).
  public static final VanillaThirdPersonPerspective FRONT =
      new VanillaThirdPersonPerspective("third_person_front", CameraType.THIRD_PERSON_FRONT);

  public boolean shouldOverrideVanillaCamera() {
    return false;
  }

  private VanillaThirdPersonPerspective(String name, CameraType cameraType) {
    super(name, cameraType);
  }
}
