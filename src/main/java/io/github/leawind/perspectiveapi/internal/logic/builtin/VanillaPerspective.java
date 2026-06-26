package io.github.leawind.perspectiveapi.internal.logic.builtin;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveStateImpl;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public sealed class VanillaPerspective implements Perspective
    permits VanillaFirstPersonPerspective, VanillaThirdPersonPerspective {

  private final Identifier id;
  private final CameraType cameraType;

  protected final PerspectiveState.Mutable state = new PerspectiveStateImpl();

  protected VanillaPerspective(String name, CameraType cameraType) {
    this.id = Bridge.createIdentifier(name);
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
  public boolean allowTransition() {
    return false;
  }

  @Override
  public @NonNull PerspectiveState getState() {
    return state;
  }

  @Override
  public String toString() {
    return id() + "{" + getClass().getSimpleName() + "}";
  }
}
