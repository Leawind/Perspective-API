package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

import static io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu.WheelMenuManager.ROTATE_OFFSET_RAD;

import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import io.github.leawind.perspectiveapi.internal.utils.smooth.ExpSmoothDouble;
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
  private static final float SELECTED_SCALE = 1.35f;

  public static @NonNull WheelMenuRenderer getInstance() {
    return INSTANCE;
  }

  private double lastRenderTime = Double.MAX_VALUE;
  private final ExpSmoothDouble smoothScale = new ExpSmoothDouble().setHalflife(0.015);
  private final ExpSmoothDouble smoothRotation = new ExpSmoothDouble().setHalflife(0.025);
  private boolean lastOpened;

  private WheelMenuRenderer() {}

  /// Called by {@link WheelMenuManager#onMouseScroll} when the list rotates.
  /// Adds one sector's worth of angular offset so the renderer can animate the rotation.
  void notifyScroll(double sectorRad, boolean clockwise) {
    double delta = clockwise ? -sectorRad : sectorRad;
    smoothRotation.setCurrent(smoothRotation.getCurrent() + delta);
  }

  public void render(DrawContext ctx, int screenWidth, int screenHeight) {

    double now = GLFW.glfwGetTime();
    double deltaTime = now - lastRenderTime;
    lastRenderTime = now;
    deltaTime = Math.max(deltaTime, 0);

    WheelMenuManager manager = WheelMenuManager.getInstance();
    boolean opened = manager.isOpened();

    // Drive smoothScale and smoothRotation toward their targets
    if (opened && !lastOpened) {
      smoothScale.setCurrent(0).setTarget(1);
    } else if (!opened && lastOpened) {
      smoothScale.setTarget(0);
    }
    lastOpened = opened;
    smoothScale.update(deltaTime);
    smoothRotation.setTarget(0);
    smoothRotation.update(deltaTime);

    double scale = smoothScale.getCurrent();
    if (scale < 0.001) return;

    WheelMenuManager.WheelMenuLayout layout =
        new WheelMenuManager.WheelMenuLayout(screenWidth, screenHeight);
    List<WheelMenuItem> items = manager.getItemList();

    if (items.isEmpty()) {
      // TODO display something
      return;
    }

    WheelMenuItem selected = manager.getSelectedItem();

    drawRingIcons(ctx, layout, items, selected, scale, smoothRotation.getCurrent());
    if (selected != null) {
      drawCenterInfo(ctx, layout, selected, scale);
    }

    if (WheelMenuManager.DEBUG) {
      drawAnchorDebug(ctx, layout);
    }
  }

  private void drawAnchorDebug(DrawContext ctx, WheelMenuManager.WheelMenuLayout layout) {
    final float size = 3;
    final float halfSize = size / 2;

    float cx = layout.center().x();
    float cy = layout.center().y();
    float ax = WheelMenuManager.getInstance().getAnchor().x();
    float ay = WheelMenuManager.getInstance().getAnchor().y();
    ctx.fill(
        (int) (cx + ax - halfSize),
        (int) (cy + ay - halfSize),
        (int) (cx + ax + halfSize),
        (int) (cy + ay + halfSize),
        1);
  }

  private void drawRingIcons(
      DrawContext ctx,
      WheelMenuManager.WheelMenuLayout layout,
      List<WheelMenuItem> items,
      WheelMenuItem selected,
      double scale,
      double rotationOffset) {
    Vector2fc center = layout.center();
    float iconRadius = layout.iconRadius() * (float) scale;

    double sectorRad = 2 * Math.PI / items.size();

    for (int i = 0; i < items.size(); i++) {
      WheelMenuItem item = items.get(i);
      boolean isSelected = item == selected;

      double itemRad = sectorRad * i + ROTATE_OFFSET_RAD + rotationOffset;
      float itemScale = isSelected ? (float) (SELECTED_SCALE * scale) : (float) scale;
      float iconSize = layout.iconSize() * itemScale;
      int iconSizeInt = (int) iconSize;
      float halfIconSize = iconSize / 2;

      float iconX = center.x() + iconRadius * (float) Math.cos(itemRad);
      float iconY = center.y() + iconRadius * (float) Math.sin(itemRad);

      int sx = (int) (iconX - halfIconSize);
      int sy = (int) (iconY - halfIconSize);

      try {
        ctx.drawGamemodeSwitcherSlot(sx, sy, iconSizeInt, iconSizeInt, COLOR_TEXT);
        if (isSelected) {
          ctx.drawGamemodeSwitcherSelection(sx, sy, iconSizeInt, iconSizeInt, COLOR_TEXT);
        }
        drawAvailabilityIndicator(ctx, item, iconX + halfIconSize, iconY + halfIconSize);

        // Icon with 5px padding (matching GameModeSwitcherScreen's 16x16 inside 26x26)
        Identifier icon = item.icon();
        if (icon != null) {
          float pad = iconSize * 0.19f; // ~5px padding for 26px slot
          int ix = (int) (iconX - halfIconSize + pad);
          int iy = (int) (iconY - halfIconSize + pad);
          int is = (int) (iconSize - pad * 2);
          ctx.blit(icon, 0, 0, ix, iy, is, is, is, is, 1.0f);
        }
      } catch (IllegalStateException e) {
        LOGGER.warn("Error occurred drawing ring icon", e);
      }
    }
  }

  private void drawAvailabilityIndicator(DrawContext ctx, WheelMenuItem item, float x, float y) {
    float r = 3;
    int color =
        switch (item.availability()) {
          case AVAILABLE -> COLOR_AVAILABLE;
          case UNAVAILABLE -> COLOR_UNAVAILABLE;
          case UNREGISTERED -> COLOR_UNREGISTERED;
        };
    ctx.fill((int) (x - r), (int) (y - r), (int) (x + r), (int) (y + r), color);
  }

  private void drawCenterInfo(
      DrawContext ctx, WheelMenuManager.WheelMenuLayout layout, WheelMenuItem item, double scale) {

    float cx = layout.center().x();
    float cy = layout.center().y();
    float centerIconSize = layout.centerIconSize();

    // Icon
    {
      float scaledIconSize = centerIconSize * (float) scale;
      float scaledHalfIconSize = scaledIconSize / 2;

      Identifier icon = item.icon();
      if (icon != null) {
        int ix = (int) (cx - scaledHalfIconSize);
        int iy = (int) (cy - scaledHalfIconSize - 6);
        int is = (int) scaledIconSize;
        ctx.blit(icon, 0, 0, ix, iy, is, is, is, is, 1.0f);
      }
    }

    // Name
    {
      float halfCenterIconSize = centerIconSize / 2;

      Font font = Minecraft.getInstance().font;
      Component text = item.displayName();
      ctx.text(
          font,
          text,
          (int) (cx - font.width(text) / 2.0f),
          (int) (cy + halfCenterIconSize + 2),
          COLOR_TEXT,
          true);
    }
  }
}
