package io.github.leawind.perspectiveapi.internal.bridge.mixin.fov;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;


/*? if <26.1 {*/
/*import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyFieldOfViewContext;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
*//*? }*/

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  /*? if >=26.1 {*/
  /*? } else if >=1.21.11 {*/
  /*@Unique private final ModifyFieldOfViewContext context = new ModifyFieldOfViewContext();
  @Unique private float perspective_api$cachedFov;

  @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getFov", at = @At(value = "RETURN"))
  private float modifyFov(float fov) {
    context.setup(fov);
    GameClientEvents.MODIFY_FIELD_OF_VIEW.emit(context);
    this.perspective_api$cachedFov = context.fieldOfView;
    return context.fieldOfView;
  }
  *//*? } else {*/
  /*@Unique private final ModifyFieldOfViewContext context = new ModifyFieldOfViewContext();

  @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getFov", at = @At(value = "RETURN"))
  private double modifyFov(double fov) {
    context.setup((float)fov);
    GameClientEvents.MODIFY_FIELD_OF_VIEW.emit(context);
    return context.fieldOfView;
  }
  *//*? }*/
}
