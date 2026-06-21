package io.github.leawind.perspectiveapi.internal.logic.vanilla;

import net.minecraft.client.CameraType;

public final class VanillaFirstPersonPerspective extends VanillaPerspective {
  public static final VanillaFirstPersonPerspective INSTANCE = new VanillaFirstPersonPerspective();

  private VanillaFirstPersonPerspective() {
    super("first_person", CameraType.FIRST_PERSON);
  }

  @Override
  public boolean shouldOverrideVanillaCamera() {
    return false;
  }
}
