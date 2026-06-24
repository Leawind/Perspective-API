package io.github.leawind.perspectiveapi.internal.bridge.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(GameRenderer.class)
public interface GameRenderAccessorMixin {
  
  /*? if <26.1 {*/
  /*@org.spongepowered.asm.mixin.gen.Accessor("fov")
  float getFov();
  *//*? }*/}
