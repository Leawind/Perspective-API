package io.github.leawind.perspectiveapi.internal.bridge.mixin.fov;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/*? if <26.1 {*/
/*import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyFieldOfViewContext;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*//*? }*/

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

  /*? if >=26.1 {*/
  /*? } else if >=1.21.11 {*/
  /*@Unique private boolean perspective_api$useFovSetting;

  @Unique
  private final ModifyFieldOfViewContext perspective_api$context = new ModifyFieldOfViewContext();

  @Inject(method = "getFov", at = @At("HEAD"))
  private void perspective_api$captureFovArgs(
      net.minecraft.client.Camera camera,
      float partialTick,
      boolean useFovSetting,
      CallbackInfoReturnable<Float> cir) {
    this.perspective_api$useFovSetting = useFovSetting;
  }

  @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getFov", at = @At("RETURN"))
  private float modifyFov(float fovDeg) {
    if (!this.perspective_api$useFovSetting) return fovDeg;
    perspective_api$context.setup(fovDeg);
    GameClientEvents.MODIFY_FIELD_OF_VIEW.emit(perspective_api$context);
    return perspective_api$context.fieldOfViewDeg;
  }
  *//*? } else if !forge {*/
  /*@Unique private boolean perspective_api$useFovSetting;

  @Unique
  private final ModifyFieldOfViewContext perspective_api$context = new ModifyFieldOfViewContext();

  @Inject(method = "getFov", at = @At("HEAD"))
  private void perspective_api$captureFovArgs(
      net.minecraft.client.Camera camera,
      float partialTick,
      boolean useFovSetting,
      CallbackInfoReturnable<Double> cir) {
    this.perspective_api$useFovSetting = useFovSetting;
  }

  @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getFov", at = @At("RETURN"))
  private double modifyFov(double fovDeg) {
    if (!this.perspective_api$useFovSetting) return fovDeg;
    perspective_api$context.setup((float) fovDeg);
    GameClientEvents.MODIFY_FIELD_OF_VIEW.emit(perspective_api$context);
    return perspective_api$context.fieldOfViewDeg;
  }
  *//*? }*/
}
