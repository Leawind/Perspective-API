package io.github.leawind.perspectiveapi.internal.bridge.gui;

import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/// Version-agnostic wrapper for GUI drawing operations.
///
/// Bridges the API differences between {@code GuiGraphics} (1.21.x and earlier)
/// and {@code GuiGraphicsExtractor} (26.x).
public final class DrawContext {
  /*? if >=26.1 {*/
  private final net.minecraft.client.gui.GuiGraphicsExtractor graphics;

  /*? } else {*/
  /*private final net.minecraft.client.gui.GuiGraphics graphics;
   *//*? }*//*? if >=26.1 {*/
  public DrawContext(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
    this.graphics = graphics;
  }

  /*? } else {*/
  /*public DrawContext(net.minecraft.client.gui.GuiGraphics graphics) {
    this.graphics = graphics;
  }
  *//*? }*/

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

  public void blit(
      Identifier texture, int u, int v, int x, int y, int w, int h, int texW, int texH) {
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
        -1);
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
