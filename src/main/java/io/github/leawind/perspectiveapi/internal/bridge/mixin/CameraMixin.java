package io.github.leawind.perspectiveapi.internal.bridge.mixin;

import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import net.minecraft.client.Camera;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Camera.class)
public abstract class CameraMixin implements CameraAccessor {
  // In Minecraft 26.1, there are 3 static fields with same type: FORWARDS, UP and LEFT
  // Due to mixin's bug, if we use @Accessor("forwards") in `CameraAccessorMixin`,
  // it will access the static field instead of the instance field.
  // So use @Shadow + interface CameraAccessor
  @Final @Shadow private Vector3f forwards;
  @Final @Shadow private Vector3f up;
  @Final @Shadow private Vector3f left;

  @Override
  public Vector3f perspective_api$forwards() {
    return forwards;
  }

  @Override
  public Vector3f perspective_api$up() {
    return up;
  }

  @Override
  public Vector3f perspective_api$left() {
    return left;
  }
}
