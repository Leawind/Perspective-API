package io.github.leawind.perspectiveapi.internal.bridge.mixin.fov;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public interface GameRendererAccessorMixin {
  /*? if >= 26.1 {*/
  /*? } else if >=1.21.11 {*/
  /*@org.spongepowered.asm.mixin.gen.Accessor("perspective_api$cachedFov")
  float perspective_api$getCachedFov();
  *//*? } else {*/
  /*@org.spongepowered.asm.mixin.gen.Accessor("fov")
  float getFov();
  *//*? }*/
}
