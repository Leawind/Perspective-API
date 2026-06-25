package io.github.leawind.perspectiveapi.internal.bridge.mixin.fov;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;

/*? if >=26.1 {*/
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyFieldOfViewContext;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
/*? }*/

@Mixin(Camera.class)
public class CameraMixin {
  /*? if >=26.1 {*/
  @Unique
  private final ModifyFieldOfViewContext modifyFieldOfViewContext = new ModifyFieldOfViewContext();

  @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
  private float modifyFov(float fov) {
    modifyFieldOfViewContext.setup(fov);
    GameClientEvents.MODIFY_FIELD_OF_VIEW.emit(modifyFieldOfViewContext);
    return modifyFieldOfViewContext.fieldOfView;
  }
  /*? }*/
}
