package io.github.leawind.perspectiveapi.internal.logic.builtin;

import net.minecraft.client.CameraType;

public final class VanillaThirdPersonPerspective extends VanillaPerspective {
  public static final VanillaThirdPersonPerspective BACK =
      new VanillaThirdPersonPerspective("third_person_back", CameraType.THIRD_PERSON_BACK);
  public static final VanillaThirdPersonPerspective FRONT =
      new VanillaThirdPersonPerspective("third_person_front", CameraType.THIRD_PERSON_FRONT);

  public boolean shouldOverrideVanillaCamera() {
    return false;
  }

  private VanillaThirdPersonPerspective(String name, CameraType cameraType) {
    super(name, cameraType);
  }
}
