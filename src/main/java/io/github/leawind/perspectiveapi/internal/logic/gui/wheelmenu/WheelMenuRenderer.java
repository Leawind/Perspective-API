package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

import static io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu.WheelMenuManager.ROTATE_OFFSET_RAD;

import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Vector2fc;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Renders the perspective wheel menu overlay.
///
/// Only the currently selected sector is highlighted with a white semi-transparent
/// filled ring-sector shape. Icons and center label are drawn on top.
public final class WheelMenuRenderer {
  private static final Logger LOGGER = LoggerFactory.getLogger(WheelMenuRenderer.class);
  private static final WheelMenuRenderer INSTANCE = new WheelMenuRenderer();

  private static final int COLOR_TEXT = 0xFF_FF_FF_FF;
  private static final int COLOR_AVAILABLE = 0x00_22_C5_5E;
  private static final int COLOR_UNAVAILABLE = 0xFF_EA_B3_08;
  private static final int COLOR_UNREGISTERED = 0xFF_EF_44_44;
  static final float SELECTED_SCALE = 1.35f;

  private WheelMenuRenderer() {}

  public static @NonNull WheelMenuRenderer getInstance() {
    return INSTANCE;
  }

  private double lastRenderTime = Double.MAX_VALUE;

  public void render(DrawContext ctx, int screenWidth, int screenHeight) {
    double now = GLFW.glfwGetTime();
    double deltaTime = now - lastRenderTime;
    lastRenderTime = now;
    deltaTime = Math.max(deltaTime, 0);

    WheelMenuManager manager = WheelMenuManager.getInstance();

    float alphaMultiplier = manager.isOpened() ? 1 : 0;

    WheelMenuManager.WheelMenuLayout layout = manager.getLayout(screenWidth, screenHeight);
    List<WheelMenuItem> items = manager.getItemList();

    if (items.isEmpty()) {
      // TODO display something
      return;
    }

    WheelMenuItem selected = manager.getSelectedItem();

    drawRingIcons(manager, deltaTime, ctx, layout, items, selected, alphaMultiplier);
    if (selected != null) {
      drawCenterInfo(ctx, layout, selected, alphaMultiplier);
    }

    if (WheelMenuManager.DEBUG) {
      drawAnchorDebug(ctx, layout, alphaMultiplier);
    }
  }

  /// Applies an alpha multiplier to a color.
  ///
  /// @param color the base color in ARGB format (e.g., `0xFF_FF_FF_FF`).
  ///     The alpha channel must be explicitly set; colors without an alpha
  ///     component (e.g., `0xFF_FF_FF`) will be treated as fully transparent.
  /// @param alpha multiplier in `[0.0, 1.0]`
  /// @return the color with adjusted alpha, in ARGB format
  @Deprecated
  private static int applyAlpha(int color, float alpha) {
    int a = (color >> 24) & 0xFF;
    int r = (color >> 16) & 0xFF;
    int g = (color >> 8) & 0xFF;
    int b = color & 0xFF;
    a = (int) (a * alpha);
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  private void drawAnchorDebug(
      DrawContext ctx, WheelMenuManager.WheelMenuLayout layout, float alpha) {
    final float size = 3;
    final float halfSize = size / 2;

    float cx = layout.centerX();
    float cy = layout.centerY();
    float ax = (float) WheelMenuManager.getInstance().getAnchor().x();
    float ay = (float) WheelMenuManager.getInstance().getAnchor().y();
    ctx.fill(
        (int) (cx + ax - halfSize),
        (int) (cy + ay - halfSize),
        (int) (cx + ax + halfSize),
        (int) (cy + ay + halfSize),
        applyAlpha(0xFF_FF_FF_00, alpha));
  }

  private void drawRingIcons(
      WheelMenuManager manager,
      double deltaTime,
      DrawContext ctx,
      WheelMenuManager.WheelMenuLayout layout,
      List<WheelMenuItem> items,
      WheelMenuItem selected,
      float alphaMultiplier) {
    Vector2fc center = layout.center();
    float iconRadius = layout.iconRadius();

    double sectorRad = 2 * Math.PI / items.size();

    for (int i = 0; i < items.size(); i++) {
      WheelMenuItem item = items.get(i);
      boolean isSelected = item == selected;

      var smoothRenderState = item.getSmoothRenderState();

      // Calculate target render state and update current
      {
        WheelMenuItem.RenderState target = smoothRenderState.target();

        target.icon = item.icon();
        target.isSelected = isSelected;

        if (manager.isOpened()) {
          double itemRad = sectorRad * i + ROTATE_OFFSET_RAD;
          target.position.set(
              iconRadius * (float) Math.cos(itemRad), iconRadius * (float) Math.sin(itemRad));

          target.scale = isSelected ? WheelMenuRenderer.SELECTED_SCALE : 1;
          target.alpha = alphaMultiplier;
        } else {
          target.position.set(0, 0);
          target.scale = 0;
          target.alpha = 0;
        }
        smoothRenderState.update(deltaTime);
      }

      var renderState = smoothRenderState.current();

      float iconSize = layout.iconSize() * renderState.scale;
      int iconSizeInt = (int) iconSize;
      float halfIconSize = iconSize / 2;

      float iconX = center.x() + renderState.position.x();
      float iconY = center.y() + renderState.position.y();

      int sx = (int) (iconX - halfIconSize);
      int sy = (int) (iconY - halfIconSize);

      try {
        ctx.drawGamemodeSwitcherSlot(
            sx, sy, iconSizeInt, iconSizeInt, applyAlpha(0xFF_FF_FF_FF, renderState.alpha));
        if (isSelected) {
          ctx.drawGamemodeSwitcherSelection(
              sx, sy, iconSizeInt, iconSizeInt, applyAlpha(0xFF_FF_FF_FF, renderState.alpha));
        }
        drawAvailabilityIndicator(
            ctx, item, iconX + halfIconSize, iconY + halfIconSize, renderState.alpha);

        // Icon with 5px padding (matching GameModeSwitcherScreen's 16x16 inside 26x26)
        Identifier icon = item.icon();
        if (icon != null) {
          float pad = iconSize * 0.19f; // ~5px padding for 26px slot
          int ix = (int) (iconX - halfIconSize + pad);
          int iy = (int) (iconY - halfIconSize + pad);
          int is = (int) (iconSize - pad * 2);
          ctx.blit(icon, 0, 0, ix, iy, is, is, is, is, renderState.alpha);
        }
      } catch (IllegalStateException e) {
        LOGGER.warn("Error occurred drawing ring icon", e);
      }
    }
  }

  private void drawAvailabilityIndicator(
      DrawContext ctx, WheelMenuItem item, float x, float y, float alpha) {
    float r = 3;
    int color =
        switch (item.availability()) {
          case AVAILABLE -> COLOR_AVAILABLE;
          case UNAVAILABLE -> COLOR_UNAVAILABLE;
          case UNREGISTERED -> COLOR_UNREGISTERED;
        };
    ctx.fill((int) (x - r), (int) (y - r), (int) (x + r), (int) (y + r), applyAlpha(color, alpha));
  }

  private void drawCenterInfo(
      DrawContext ctx, WheelMenuManager.WheelMenuLayout layout, WheelMenuItem item, float alpha) {

    float cx = layout.centerX();
    float cy = layout.centerY();
    float centerIconSize = layout.centerIconSize();
    float halfCenterIconSize = layout.centerIconSize() / 2;

    // Icon
    {
      Identifier icon = item.icon();
      if (icon != null) {
        int ix = (int) (cx - halfCenterIconSize);
        int iy = (int) (cy - halfCenterIconSize - 6);
        int is = (int) centerIconSize;
        ctx.blit(icon, 0, 0, ix, iy, is, is, is, is, alpha);
      }
    }

    // Name
    {
      Font font = Minecraft.getInstance().font;

      Component text = item.displayName();
      ctx.text(
          font,
          text,
          (int) (cx - font.width(text) / 2.0f),
          (int) (cy + halfCenterIconSize + 2),
          applyAlpha(COLOR_TEXT, alpha),
          true);
    }
  }
}
