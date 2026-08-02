package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers;

import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import net.minecraft.resources.Identifier;

public final class AvailabilityIndicatorRenderer {
  private static final Identifier UNAVAILABLE_SPRITE =
      Bridge.parseIdentifier("perspective_api:textures/gui/sprites/hud/unavailable.png");
  private static final Identifier UNREGISTERED_SPRITE =
      Bridge.parseIdentifier("perspective_api:textures/gui/sprites/hud/unregistered.png");

  private static final float SIZE = 0.75f;
  private static final float OFFSET = 0.25f;

  private AvailabilityIndicatorRenderer() {}

  public static void drawUnavailable(
      DrawContext canvas, float iconCenterX, float iconCenterY, float iconSize) {
    draw(canvas, UNAVAILABLE_SPRITE, iconCenterX, iconCenterY, iconSize);
  }

  public static void drawUnregistered(
      DrawContext canvas, float iconCenterX, float iconCenterY, float iconSize) {
    draw(canvas, UNREGISTERED_SPRITE, iconCenterX, iconCenterY, iconSize);
  }

  private static void draw(
      DrawContext canvas,
      Identifier sprite,
      float iconCenterX,
      float iconCenterY,
      float iconSize) {
    float centerX = iconCenterX + iconSize * OFFSET;
    float centerY = iconCenterY + iconSize * OFFSET;
    float size = iconSize * SIZE;
    float halfSize = size / 2;
    int sizeInt = (int) size;
    canvas.blit(
        sprite,
        0,
        0,
        (int) (centerX - halfSize),
        (int) (centerY - halfSize),
        sizeInt,
        sizeInt,
        sizeInt,
        sizeInt,
        1);
  }
}
