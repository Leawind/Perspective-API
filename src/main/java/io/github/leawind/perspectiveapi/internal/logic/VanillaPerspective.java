package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.platform.api.Services;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public final class VanillaPerspective extends AbstractPerspective {

  private final Identifier id;
  private final CameraType cameraType;

  private VanillaPerspective(String name, CameraType cameraType) {
    this.id = Services.PLATFORM_HELPER.createIdentifier(name);
    this.cameraType = cameraType;
  }

  @SuppressWarnings("deprecation")
  public boolean shouldOverrideVanillaCamera() {
    return false;
  }

  @Override
  public @NonNull Identifier id() {
    return id;
  }

  @Override
  public @NonNull CameraType cameraType() {
    return cameraType;
  }

  public static final VanillaPerspective FIRST_PERSON =
      new VanillaPerspective("first_person", CameraType.FIRST_PERSON);

  public static final VanillaPerspective THIRD_PERSON_BACK =
      new VanillaPerspective("third_person_back", CameraType.THIRD_PERSON_BACK);

  public static final VanillaPerspective THIRD_PERSON_FRONT =
      new VanillaPerspective("third_person_front", CameraType.THIRD_PERSON_FRONT);
}
