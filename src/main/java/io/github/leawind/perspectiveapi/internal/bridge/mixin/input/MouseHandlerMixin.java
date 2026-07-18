package io.github.leawind.perspectiveapi.internal.bridge.mixin.input;

import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.MouseInputContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
  @Shadow private double xpos;
  @Shadow private double ypos;
  @Shadow private double accumulatedDX;
  @Shadow private double accumulatedDY;

  @Unique private final MouseInputContext perspectiveApi$mouseContext = new MouseInputContext();

  /*? if >=1.21.11 {*/
  @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
  private void perspectiveApi$onButton(
      long handle,
      net.minecraft.client.input.MouseButtonInfo rawButtonInfo,
      int action,
      CallbackInfo ci) {
    perspectiveApi$mouseContext.setupButton(rawButtonInfo.button(), action);
    GameClientEvents.MOUSE_INPUT.emit(perspectiveApi$mouseContext);
    if (perspectiveApi$mouseContext.consumed) ci.cancel();
  }
  /*? } else {*/
  /*@Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
  private void perspectiveApi$onPress(
      long handle, int button, int action, int modifiers, CallbackInfo ci) {
    perspectiveApi$mouseContext.setupButton(button, action);
    GameClientEvents.MOUSE_INPUT.emit(perspectiveApi$mouseContext);
    if (perspectiveApi$mouseContext.consumed) ci.cancel();
  }
  *//*? }*/

  @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
  private void perspectiveApi$onScroll(
      long handle, double xoffset, double yoffset, CallbackInfo ci) {
    perspectiveApi$mouseContext.setupScroll(yoffset);
    GameClientEvents.MOUSE_INPUT.emit(perspectiveApi$mouseContext);
    if (perspectiveApi$mouseContext.consumed) ci.cancel();
  }

  @SuppressWarnings("ConstantConditions")
  @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
  private void perspectiveApi$onMove(long handle, double xpos, double ypos, CallbackInfo ci) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.getWindow() != null) {
      double scale = minecraft.getWindow().getGuiScale();
      perspectiveApi$mouseContext.setupMove(xpos / scale, ypos / scale);
    } else {
      perspectiveApi$mouseContext.setupMove(xpos, ypos);
    }
    GameClientEvents.MOUSE_INPUT.emit(perspectiveApi$mouseContext);
    if (perspectiveApi$mouseContext.consumed) {
      this.xpos = xpos;
      this.ypos = ypos;
      ci.cancel();
    }
  }

  /*? if >=1.20.6 {*/
  @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"), cancellable = true)
  private void perspectiveApi$beforeHandleAccumulatedMovement(CallbackInfo ci) {
    if (perspectiveApi$mouseContext.consumed) {
      this.accumulatedDX = 0.0;
      this.accumulatedDY = 0.0;
      ci.cancel();
    }
  }
  /*? } else {*/
  /*@Inject(method = "turnPlayer", at = @At("HEAD"))
  private void perspectiveApi$beforeTurnPlayer(CallbackInfo ci) {
    if (perspectiveApi$mouseContext.consumed) {
      this.accumulatedDX = 0.0;
      this.accumulatedDY = 0.0;
    }
  }
  *//*? }*/
}
