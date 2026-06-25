package io.github.leawind.perspectiveapi.internal.bridge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.bridge.events.CameraSetupContext;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyFieldOfViewContext;
import net.minecraft.client.Camera;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin implements CameraAccessor {
  // region conflict fields

  // These inst field name might conflict with static field name
  // Due to mixin's bug, we can't use CameraAccessorMixin to access them.
  // So use @Shadow + interface
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

  // endregion

  /*? if >=26.1 {*/
  @Unique private static final String SETUP_CAMERA_METHOD = "alignWithEntity";
  /*? } else {*/
  /*@Unique private static final String SETUP_CAMERA_METHOD = "setup";
   */
  /*? } */

  // region setup camera
  @Unique private final CameraSetupContext cameraSetupContext = new CameraSetupContext();

  /*? if >=26.1 {*/
  @Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"))
  private void beforeCameraUpdate(float partialTicks, CallbackInfo ci) {
    Camera camera = (Camera) (Object) this;
    if (!camera.isInitialized()) return;

    cameraSetupContext.setup(camera, partialTicks);
    GameClientEvents.SETUP_CAMERA.emit(cameraSetupContext);
  }

  /*? } else {*/
  /*@Inject(method = SETUP_CAMERA_METHOD, at = @At("RETURN"), cancellable = true)
  private void beforeMoveCamera(
    /^? if >= 1.21.11 {^/
    net.minecraft.world.level.Level blockGetter,
    /^? } else {^/
    /^net.minecraft.world.level.BlockGetter blockGetter,
    ^//^? }^/
    net.minecraft.world.entity.Entity entity,
    boolean detached,
    boolean mirror,
    float partialTicks,
    CallbackInfo ci) {
    cameraSetupContext.setup((Camera) (Object) this, partialTicks);
    GameClientEvents.SETUP_CAMERA.emit(cameraSetupContext);
  }
  */
  /*? } */

  // endregion

}
