package io.github.leawind.perspectiveapi.internal.bridge.gui;

import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/// Version-agnostic wrapper for GUI drawing operations.
///
/// ### Coordinate Convension
///
/// ```
/// +------>
/// |     x+
/// |
/// v  y+
/// ```
public final class DrawContext {
  private final net.minecraft.client.gui.GuiGraphicsExtractor graphics;

  public DrawContext(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
    this.graphics = graphics;
  }

  public void fill(int x0, int y0, int x1, int y1, int color) {
    graphics.fill(x0, y0, x1, y1, color);
  }

  public void text(Font font, Component text, int x, int y, int color, boolean shadow) {
    /*? if >=26.1 {*/
    graphics.text(font, text, x, y, color, shadow);
    /*? } else {*/
    /*graphics.drawString(font, text, x, y, color, shadow);
     *//*? }*/
  }

  /// Renders a tooltip using Minecraft's native tooltip renderer.
  public void tooltip(Font font, List<Component> lines, int mouseX, int mouseY) {
    /*? if >=26.1 {*/
    graphics.nextStratum();
    graphics.tooltip(
        font,
        lines.stream()
            .map(Component::getVisualOrderText)
            .map(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent::create)
            .toList(),
        mouseX,
        mouseY,
        net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE,
        null);
    /*? } else if >=1.21.11 {*/
    /*graphics.renderTooltip(
        font,
        lines.stream()
            .map(Component::getVisualOrderText)
            .map(net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent::create)
            .toList(),
        mouseX,
        mouseY,
        net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE,
        null);
    *//*? } else {*/
    /*graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
     *//*? }*/
  }

  public void blit(
      Identifier texture, int u, int v, int x, int y, int w, int h, int texW, int texH, float alpha) {
    if (alpha == 0) return;

    /*? if >=1.21.11 {*/
    graphics.blit(
        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
        texture,
        x,
        y,
        (float) u,
        (float) v,
        w,
        h,
        texW,
        texH,
        net.minecraft.util.ARGB.color(alpha, 0xFFFFFF));
    /*? } else {*/
    /*graphics.blit(texture, x, y, 0, (float) u, (float) v, w, h, texW, texH);
     *//*? }*/
  }

  private static final Identifier SLOT_SPRITE =
      Bridge.createIdentifier("minecraft", "gamemode_switcher/slot");
  private static final Identifier SELECTION_SPRITE =
      Bridge.createIdentifier("minecraft", "gamemode_switcher/selection");
  private static final Identifier GAMEMODE_SWITCHER_LOCATION =
      Bridge.createIdentifier("minecraft","textures/gui/container/gamemode_switcher.png");

  /// Draws a slot from the gamemode switcher texture.
  ///
  /// Pre-1.20.4: uses hardcoded UV coordinates from `gamemode_switcher.png`.
  /// 1.20.4+: uses `blitSprite` API.
  /// 1.21.11+: uses `blitSprite` with color support.
  public void drawGamemodeSwitcherSlot(int x, int y, int w, int h, int color) {
    /*? if >=1.21.11 {*/
    graphics.blitSprite(
        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, x, y, w, h, color);
    /*? } else if >=1.20.4 {*/
    /*graphics.blitSprite(SLOT_SPRITE, x, y, w, h);
     *//*? } else {*/
    /*graphics.blit(GAMEMODE_SWITCHER_LOCATION, x, y, w, h, 0, 75f, 26, 26, 128, 128);
     *//*? }*/
  }

  public void drawGamemodeSwitcherSelection(int x, int y, int w, int h, int color) {
    /*? if >=1.21.11 {*/
    graphics.blitSprite(
        net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
        SELECTION_SPRITE,
        x,
        y,
        w,
        h,
        color);
    /*? } else if >=1.20.4 {*/
    /*graphics.blitSprite(SELECTION_SPRITE, x, y, w, h);
     *//*? } else {*/
    /*graphics.blit(GAMEMODE_SWITCHER_LOCATION, x, y, w, h, 26f, 75f, 26, 26, 128, 128);
     *//*? }*/
  }
}
