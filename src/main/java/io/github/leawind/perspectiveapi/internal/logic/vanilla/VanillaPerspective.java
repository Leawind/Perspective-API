package io.github.leawind.perspectiveapi.internal.logic.vanilla;

import io.github.leawind.perspectiveapi.internal.logic.AbstractPerspective;
import io.github.leawind.perspectiveapi.platform.api.Services;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public sealed class VanillaPerspective extends AbstractPerspective
    permits VanillaFirstPersonPerspective, VanillaThirdPersonPerspective {

  private final Identifier id;
  private final CameraType cameraType;

  protected VanillaPerspective(String name, CameraType cameraType) {
    this.id = Services.PLATFORM_HELPER.createIdentifier(name);
    this.cameraType = cameraType;
  }

  @Override
  public boolean allowTransition() {
    return true;
  }

  @Override
  public @NonNull Identifier id() {
    return id;
  }

  @Override
  public @NonNull CameraType cameraType() {
    return cameraType;
  }
}
