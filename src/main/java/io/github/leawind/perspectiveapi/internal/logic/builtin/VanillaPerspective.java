package io.github.leawind.perspectiveapi.internal.logic.builtin;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.platform.api.Services;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

public sealed class VanillaPerspective implements Perspective
    permits VanillaFirstPersonPerspective, VanillaThirdPersonPerspective {

  private final Identifier id;
  private final CameraType cameraType;

  protected final Vector3d position = new Vector3d();
  protected final Quaternionf rotation = new Quaternionf();

  protected VanillaPerspective(String name, CameraType cameraType) {
    this.id = Services.PLATFORM_HELPER.createIdentifier(name);
    this.cameraType = cameraType;
  }

  @Override
  public final @NonNull Identifier id() {
    return id;
  }

  @Override
  public final @NonNull CameraType cameraType() {
    return cameraType;
  }

  @Override
  public final @NonNull Vector3dc getPosition() {
    return position;
  }

  @Override
  public final @NonNull Quaternionfc getRotation() {
    return rotation;
  }

  @Override
  public boolean allowTransition() {
    return true;
  }

  @Override
  public String toString() {
    return id() + "{" + getClass().getSimpleName() + "}";
  }
}
