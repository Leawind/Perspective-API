package io.github.leawind.perspectiveapi.internal.bridge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.context.CameraSetupContext;
import io.github.leawind.perspectiveapi.internal.bridge.events.context.ModifyFiedOfViewContext;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
  // region setup camera
  @Unique private final CameraSetupContext cameraSetupContext = new CameraSetupContext();

  /*? if >=26.1 {*/
  @Inject(method = "alignWithEntity", at = @At("RETURN"), cancellable = true)
  private void beforeCameraUpdate(float partialTicks, CallbackInfo ci) {
    Camera camera = (Camera) (Object) this;

    if (!camera.isInitialized()) {
      return;
    }

    cameraSetupContext.setup(camera, partialTicks);
    GameClientEvents.SETUP_CAMERA.emit(cameraSetupContext);

    if (cameraSetupContext.cancelDefault) {
      ci.cancel();
    }
  }

  /*? } else {*/
  /*@Inject(
      method = "setup",
      at = {
        /^? if >= 1.21 {^/
        @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;move(FFF)V",
            ordinal = 0,
            shift = At.Shift.BEFORE),
        @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;move(FFF)V",
            ordinal = 1,
            shift = At.Shift.BEFORE)
        /^? } else {^/
        /^@At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/Camera;move(DDD)V",
          ordinal = 0,
          shift = At.Shift.BEFORE),
        @At(
          value = "INVOKE",
          target = "Lnet/minecraft/client/Camera;move(DDD)V",
          ordinal = 1,
          shift = At.Shift.BEFORE)
        ^//^? }^/
      },
      cancellable = true)
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
    if (cameraSetupContext.cancelDefault) {
      ci.cancel();
    }
  }
  *//*? } */

  // endregion

  // region modify field of view

  /*? if >=26.1 {*/
  @Unique
  private final ModifyFiedOfViewContext modifyFiedOfViewContext = new ModifyFiedOfViewContext();

  @ModifyReturnValue(method = "calculateFov", at = @At(value = "RETURN"))
  private float modifyFov(float fov) {
    modifyFiedOfViewContext.setup(fov);
    GameClientEvents.MODIFY_FIELD_OF_VIEW.emit(modifyFiedOfViewContext);
    return modifyFiedOfViewContext.fieldOfView;
  }
  /*? }*/

  // endregion
}
