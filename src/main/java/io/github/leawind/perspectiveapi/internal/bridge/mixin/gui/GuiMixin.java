package io.github.leawind.perspectiveapi.internal.bridge.mixin.gui;

import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.GuiRenderContext;
import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
  @Shadow @Final private Minecraft minecraft;

  @Unique private final GuiRenderContext perspectiveApi$guiRenderContext = new GuiRenderContext();

  /*? if >=26.2 {*/
  @Shadow @Final private net.minecraft.client.renderer.state.gui.GuiRenderState guiRenderState;

  @Inject(method = "extractRenderState", at = @At("TAIL"))
  private void perspectiveApi$afterExtractRenderState(
      net.minecraft.client.DeltaTracker deltaTracker,
      boolean shouldRenderLevel,
      boolean resourcesLoaded,
      CallbackInfo ci) {
    var window = this.minecraft.getWindow();
    if (window == null) return;
    int xMouse = (int) minecraft.mouseHandler.getScaledXPos(window);
    int yMouse = (int) minecraft.mouseHandler.getScaledYPos(window);
    var graphics =
        new net.minecraft.client.gui.GuiGraphicsExtractor(
            minecraft, this.guiRenderState, xMouse, yMouse);
    perspectiveApi$guiRenderContext.setup(
        new DrawContext(graphics), window.getGuiScaledWidth(), window.getGuiScaledHeight());
    GameClientEvents.RENDER_GUI_OVERLAY.emit(perspectiveApi$guiRenderContext);
  }
  /*? } else if >=26.1 {*/
  /*@Inject(method = "extractRenderState", at = @At("TAIL"))
  private void perspectiveApi$afterExtractRenderState(
      net.minecraft.client.gui.GuiGraphicsExtractor graphics,
      net.minecraft.client.DeltaTracker deltaTracker,
      CallbackInfo ci) {
    var window = this.minecraft.getWindow();
    if (window == null) return;
    perspectiveApi$guiRenderContext.setup(
        new DrawContext(graphics), window.getGuiScaledWidth(), window.getGuiScaledHeight());
    GameClientEvents.RENDER_GUI_OVERLAY.emit(perspectiveApi$guiRenderContext);
  }
  *//*? } else if >=1.21 {*/
  /*@Inject(method = "render", at = @At("TAIL"))
  private void perspectiveApi$afterRender(
      net.minecraft.client.gui.GuiGraphics graphics,
      net.minecraft.client.DeltaTracker deltaTracker,
      CallbackInfo ci) {
    var window = this.minecraft.getWindow();
    if (window == null) return;
    perspectiveApi$guiRenderContext.setup(
        new DrawContext(graphics), window.getGuiScaledWidth(), window.getGuiScaledHeight());
    GameClientEvents.RENDER_GUI_OVERLAY.emit(perspectiveApi$guiRenderContext);
  }
  *//*? } else {*/
  /*@Inject(method = "render", at = @At("TAIL"))
  private void perspectiveApi$afterRender(
      net.minecraft.client.gui.GuiGraphics graphics,
      float partialTick,
      CallbackInfo ci) {
    var window = this.minecraft.getWindow();
    if (window == null) return;
    perspectiveApi$guiRenderContext.setup(
        new DrawContext(graphics), window.getGuiScaledWidth(), window.getGuiScaledHeight());
    GameClientEvents.RENDER_GUI_OVERLAY.emit(perspectiveApi$guiRenderContext);
  }
  *//*? }*/
}
