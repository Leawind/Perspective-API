package io.github.leawind.perspectiveapi.internal.bridge.mixin;

import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

  @Inject(method = "handleKeybinds", at = @At(value = "HEAD"))
  private void beforeHandleKeybinds(CallbackInfo ci) {
    GameClientEvents.HANDLE_KEYBINDS_START.emit((Minecraft) (Object) this);
  }
}
