package io.github.leawind.perspectiveapi.internal.bridge.mixin.cameraupdate;

import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  /*? if >=26.1 {*/
  @Unique private static final String CAMERA_UPDATE_METHOD = "update";
  /*? } else if >=1.21.11 {*/
  /*@Unique private static final String CAMERA_UPDATE_METHOD = "updateCamera";
   *//*? } else {*/
  /*@Unique private static final String CAMERA_UPDATE_METHOD = "renderLevel";
   *//*? } */

  @Inject(method = CAMERA_UPDATE_METHOD, at = @At("HEAD"))
  private void beforeMainCameraUpdate(CallbackInfo ci) {
    GameClientEvents.BEFORE_MAIN_CAMERA_UPDATE.emit();
  }
}
