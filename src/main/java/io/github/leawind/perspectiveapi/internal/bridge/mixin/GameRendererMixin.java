package io.github.leawind.perspectiveapi.internal.bridge.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/*? if <=1.21.11 {*/
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.context.ModifyFiedOfViewContext;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
*//*? }*/


/// TODO >= 1.21.11 ?: (1.20.4, 1.21.11]
@Mixin(value = GameRenderer.class)
public class GameRendererMixin {
  /*? if >=26.1 {*/
  /*? } else if >=1.21.11 {*/
  /*@Unique private final ModifyFiedOfViewContext context = new ModifyFiedOfViewContext();

  @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getFov", at = @At(value = "RETURN"))
  private float modifyFov(float fov) {
    context.setup(fov);
    GameClientEvents.MODIFY_FIELD_OF_VIEW.emit(context);
    return context.fieldOfView;
  }
  *//*? } else {*/
  /*@Unique private final ModifyFiedOfViewContext context = new ModifyFiedOfViewContext();

  @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "getFov", at = @At(value = "RETURN"))
  private double modifyFov(double fov) {
    context.setup((float)fov);
    GameClientEvents.MODIFY_FIELD_OF_VIEW.emit(context);
    return context.fieldOfView;
  }
  *//*? }*/
}
