package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.wheel;

import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import io.github.leawind.perspectiveapi.internal.utils.smooth.ExpSmoothDouble;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Renders the perspective wheel menu overlay.
///
/// Only the currently selected sector is highlighted with a white
/// semi-transparent filled ring-sector shape. Icons and center label are
/// drawn on top.
public final class WheelMenuRenderer {
  private static final Logger LOGGER = LoggerFactory.getLogger(WheelMenuRenderer.class);

  private static final int COLOR_TEXT = 0xFF_FF_FF_FF;

  private static final int COLOR_AVAILABLE = 0x00_22_C5_5E;
  private static final int COLOR_UNAVAILABLE = 0xFF_EA_B3_08;
  private static final int COLOR_UNREGISTERED = 0xFF_EF_44_44;
  private static final float SELECTED_SCALE = 1.35f;

  /// Rotation offset: top of circle = -PI/2.
  public static final double ROTATE_OFFSET_RAD = -Math.PI / 2;

  private double lastRenderTime = Double.MAX_VALUE;
  private final ExpSmoothDouble smoothScale = new ExpSmoothDouble().setHalflife(0.015);
  private final ExpSmoothDouble smoothRotation = new ExpSmoothDouble().setHalflife(0.015);

  private static final double ITEM_SCALE_HALFLIFE = 0.015;
  private final Map<String, ExpSmoothDouble> smoothItemScales = new HashMap<>();

  /// Called when the list rotates. Adds one sector's worth of angular offset
  /// so the renderer can animate the rotation.
  void notifyScroll(double sectorRad, boolean clockwise) {
    double delta = clockwise ? -sectorRad : sectorRad;
    smoothRotation.setCurrent(smoothRotation.getCurrent() + delta);
  }

  /// Called when the menu opens. Triggers the scale-in animation.
  public void onOpen() {
    smoothScale.setCurrent(0).setTarget(1);
  }

  /// Called when the menu closes. Triggers the scale-out animation.
  public void onClose() {
    smoothScale.setTarget(0);
  }

  /// Returns whether the menu is currently animating (scale > threshold).
  public boolean isAnimating() {
    return smoothScale.getCurrent() > 0.001;
  }

  public void render(
      DrawContext ctx,
      int screenWidth,
      int screenHeight,
      @NonNull List<WheelMenuItem> items,
      @Nullable String selectedId) {
    double now = GLFW.glfwGetTime();
    double deltaTime = now - lastRenderTime;
    lastRenderTime = now;
    deltaTime = Math.max(deltaTime, 0);

    // Drive smoothScale and smoothRotation toward their targets
    smoothScale.update(deltaTime);
    smoothRotation.setTarget(0);
    smoothRotation.update(deltaTime);

    // Per-item scale animators keyed by item id
    for (var item : items) {
      smoothItemScales
          .computeIfAbsent(
              item.id(),
              ignored -> new ExpSmoothDouble().setHalflife(ITEM_SCALE_HALFLIFE).setCurrent(1))
          .setTarget(item.id().equals(selectedId) ? SELECTED_SCALE : 1)
          .update(deltaTime);
    }

    double scale = smoothScale.getCurrent();
    if (scale < 0.001) return;

    WheelMenuLayout layout = new WheelMenuLayout(screenWidth, screenHeight);

    if (items.isEmpty()) return;

    WheelMenuItem selected = null;
    if (selectedId != null) {
      for (WheelMenuItem item : items) {
        if (item.id().equals(selectedId)) {
          selected = item;
          break;
        }
      }
    }

    drawRingIcons(ctx, layout, items, selected, scale, smoothRotation.getCurrent());
    if (selected != null) {
      drawCenterInfo(ctx, layout, selected, scale);
    }
  }

  private void drawRingIcons(
      DrawContext ctx,
      WheelMenuLayout layout,
      @NonNull List<WheelMenuItem> items,
      @Nullable WheelMenuItem selected,
      double scale,
      double rotationOffsetRad) {
    Vector2fc center = layout.center();
    float iconRadius = layout.iconRadius() * (float) scale;

    double sectorRad = 2 * Math.PI / items.size();

    for (int i = 0; i < items.size(); i++) {
      WheelMenuItem item = items.get(i);
      boolean isSelected = item == selected;

      double itemRad = sectorRad * i + ROTATE_OFFSET_RAD + rotationOffsetRad;
      float itemScale = (float) (smoothItemScales.get(item.id()).getCurrent() * scale);
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

        Identifier icon = item.icon();
        if (icon != null) {
          float pad = iconSize * 0.19f;
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
      DrawContext ctx, WheelMenuLayout layout, WheelMenuItem item, double scale) {
    float cx = layout.center().x();
    float cy = layout.center().y();
    float centerIconSize = layout.centerIconSize();

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

  /// Layout data for positioning menu elements on screen.
  public record WheelMenuLayout(Vector2f center, int minEdge) {

    public WheelMenuLayout(int width, int height) {
      this(new Vector2f((float) width / 2f, (float) height / 2f), Math.min(width, height));
    }

    public float iconRadius() {
      return minEdge * 0.25f;
    }

    public float iconSize() {
      return minEdge * 0.08f;
    }

    public float centerIconSize() {
      return minEdge * 0.1f;
    }
  }
}
