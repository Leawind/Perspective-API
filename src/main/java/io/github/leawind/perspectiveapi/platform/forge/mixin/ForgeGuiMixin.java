package io.github.leawind.perspectiveapi.platform.forge.mixin;

/*? if forge {*/
/*import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.GuiRenderContext;
import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Mixin for ForgeGui to handle GUI overlay rendering in Forge 1.20.1.
///
/// In Forge 1.20.1, `Gui.render()` is overridden by `ForgeGui` without calling super,
/// so the standard `GuiMixin` injection doesn't fire. This mixin targets `ForgeGui` directly.
@Mixin(ForgeGui.class)
public abstract class ForgeGuiMixin {

  @Unique private final GuiRenderContext perspectiveApi$guiRenderContext = new GuiRenderContext();

  @Inject(method = "render", at = @At("TAIL"))
  private void perspectiveApi$afterRender(
      net.minecraft.client.gui.GuiGraphics graphics,
      float partialTick,
      CallbackInfo ci) {
    ForgeGui self = (ForgeGui) (Object) this;
    var minecraft = self.getMinecraft();
    var window = minecraft.getWindow();
    if (window == null) return;
    perspectiveApi$guiRenderContext.setup(
        new DrawContext(graphics), window.getGuiScaledWidth(), window.getGuiScaledHeight());
    GameClientEvents.RENDER_GUI_OVERLAY.emit(perspectiveApi$guiRenderContext);
  }
}
*//*? }*/
