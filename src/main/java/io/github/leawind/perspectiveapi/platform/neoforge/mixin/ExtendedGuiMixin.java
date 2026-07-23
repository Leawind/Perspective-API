/// Mixin for ExtendedGui to handle GUI overlay rendering in NeoForge 1.20.4.
///
/// In NeoForge 1.20.4, `Gui.render()` is overridden by `ExtendedGui` without calling super,
/// so the standard `GuiMixin` injection doesn't fire. This mixin targets `ExtendedGui` directly.
package io.github.leawind.perspectiveapi.platform.neoforge.mixin;

/*? if neoforge && <=1.20.4 {*/
/*import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.GuiRenderContext;
import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.neoforged.neoforge.client.gui.overlay.ExtendedGui", remap = false)
public abstract class ExtendedGuiMixin {

  @Unique private final GuiRenderContext perspectiveApi$guiRenderContext = new GuiRenderContext();

  @Inject(method = "render", at = @At("TAIL"))
  private void perspectiveApi$afterRender(
      net.minecraft.client.gui.GuiGraphics graphics,
      float partialTick,
      CallbackInfo ci) {
    var minecraft = net.minecraft.client.Minecraft.getInstance();
    var window = minecraft.getWindow();
    if (window == null) return;
    perspectiveApi$guiRenderContext.setup(
        new DrawContext(graphics), window.getGuiScaledWidth(), window.getGuiScaledHeight());
    GameClientEvents.RENDER_GUI_OVERLAY.emit(perspectiveApi$guiRenderContext);
  }
}
*//*? } else {*/
@org.spongepowered.asm.mixin.Mixin(net.minecraft.client.Minecraft.class)
public abstract class ExtendedGuiMixin {}
/*? }*/
