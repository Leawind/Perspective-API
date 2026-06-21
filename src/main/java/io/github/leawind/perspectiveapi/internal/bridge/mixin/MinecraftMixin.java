package io.github.leawind.perspectiveapi.internal.bridge.mixin;

import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

  @Inject(method = "tick", at = @At(value = "HEAD"))
  private void beforeTick(CallbackInfo ci) {
    GameClientEvents.CLIENT_TICK_START.emit((Minecraft) (Object) this);
  }

  @Inject(method = "handleKeybinds", at = @At(value = "HEAD"))
  private void beforeHandleKeybinds(CallbackInfo ci) {
    GameClientEvents.HANDLE_KEYBINDS_START.emit((Minecraft) (Object) this);
  }

  @Inject(
      method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;)V",
      at = @At("TAIL"))
  private void afterClientLevelChange(ClientLevel level, CallbackInfo ci) {
    if (level != null) {
      GameClientEvents.AFTER_CLIENT_LEVEL_CHANGE.emit(level);
    }
  }
}
