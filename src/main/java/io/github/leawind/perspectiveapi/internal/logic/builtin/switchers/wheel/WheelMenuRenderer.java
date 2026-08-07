package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.wheel;

import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.AvailabilityIndicatorRenderer;
import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import io.github.leawind.perspectiveapi.internal.utils.WheelAnchor;
import io.github.leawind.perspectiveapi.internal.utils.smooth.ExpSmoothDouble;
import io.github.leawind.perspectiveapi.platform.api.Services;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Vector2fc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Renders the perspective wheel menu overlay.
///
/// Only the currently selected sector is highlighted with a white semi-transparent filled
/// ring-sector shape. Icons and center label are drawn on top.
public final class WheelMenuRenderer {
  private static final Logger LOGGER = LoggerFactory.getLogger(WheelMenuRenderer.class);
  private static final Sanitizer.ThrottledAction DRAW_ERROR_LOG =
      new Sanitizer.ThrottledAction(5000);

  // region style

  private static final Identifier DEFAULT_ICON =
      Bridge.parseIdentifier("perspective_api:textures/perspective/default.png");

  private static final int COLOR_TEXT = 0xFF_FF_FF_FF;

  private static final int COLOR_UNAVAILABLE = 0xFF_EA_B3_08;
  private static final int COLOR_UNREGISTERED = 0xFF_EF_44_44;
  private static final float SELECTED_SCALE = 1.35f;

  /// unit: vmin
  private static final float RING_RADIUS = 0.25f;

  /// unit: vmin
  private static final float RING_ICON_SIZE = 0.08f;

  /// unit: vmin
  private static final float CENTER_ICON_SIZE = 0.10f;

  /// Rotation offset: top of circle = -PI/2.
  public static final double ROTATE_OFFSET_RAD = -Math.PI / 2;

  // endregion

  private double lastRenderTime = Double.MAX_VALUE;

  /// `[0, 1]`
  private final ExpSmoothDouble smoothScale = new ExpSmoothDouble().setHalflife(0.015);

  private final ExpSmoothDouble smoothRotation = new ExpSmoothDouble().setHalflife(0.015);

  private static final double ITEM_SCALE_HALFLIFE = 0.015;
  private final Map<String, ExpSmoothDouble> smoothItemScales = new HashMap<>();

  /// Called when the list rotates. Adds one sector's worth of angular offset so the renderer can
  /// animate the rotation.
  void notifyScroll(double sectorRad, boolean clockwise) {
    double delta = clockwise ? -sectorRad : sectorRad;
    smoothRotation.setCurrent(smoothRotation.getCurrent() + delta);
  }

  /// Called when the menu opens. Triggers the scale-in animation.
  public void onOpen() {
    smoothScale.setTarget(1);
  }

  /// Called when the menu closes. Triggers the scale-out animation.
  public void onClose() {
    smoothScale.setTarget(0);
  }

  public void render(
      DrawContext ctx,
      WheelMenu.Layout layout,
      @NonNull WheelAnchor anchor,
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

    if (Services.PLATFORM_HELPER.isDevelopmentEnvironment()) {
      drawDebugAnchor(ctx, layout, anchor);
    }
  }

  private void drawRingIcons(
      DrawContext ctx,
      WheelMenu.Layout layout,
      @NonNull List<WheelMenuItem> items,
      @Nullable WheelMenuItem selected,
      double scale,
      double rotationOffsetRad) {
    Vector2fc center = layout.center();
    float ringRadius = layout.vmin(RING_RADIUS) * (float) scale;

    double sectorRad = 2 * Math.PI / items.size();

    for (int i = 0; i < items.size(); i++) {
      WheelMenuItem item = items.get(i);
      boolean isSelected = item == selected;

      double itemRad = sectorRad * i + ROTATE_OFFSET_RAD + rotationOffsetRad;
      float itemScale = (float) (smoothItemScales.get(item.id()).getCurrent() * scale);
      float iconSize = layout.vmin(RING_ICON_SIZE) * itemScale;
      int iconSizeInt = (int) iconSize;
      float halfIconSize = iconSize / 2;

      float iconX = center.x() + ringRadius * (float) Math.cos(itemRad);
      float iconY = center.y() + ringRadius * (float) Math.sin(itemRad);

      int sx = (int) (iconX - halfIconSize);
      int sy = (int) (iconY - halfIconSize);

      try {
        ctx.drawGamemodeSwitcherSlot(sx, sy, iconSizeInt, iconSizeInt, COLOR_TEXT);
        if (isSelected) {
          ctx.drawGamemodeSwitcherSelection(sx, sy, iconSizeInt, iconSizeInt, COLOR_TEXT);
        }

        Identifier icon = item.icon();
        if (icon == null) {
          icon = DEFAULT_ICON;
        }

        float pad = iconSize * 0.19f;
        int ix = (int) (iconX - halfIconSize + pad);
        int iy = (int) (iconY - halfIconSize + pad);
        int is = (int) (iconSize - pad * 2);
        ctx.blit(icon, 0, 0, ix, iy, is, is, is, is, 1);

        drawAvailabilityIndicator(ctx, item, iconX, iconY, iconSize);
      } catch (IllegalStateException e) {
        DRAW_ERROR_LOG.run(
            item.id(), () -> LOGGER.warn("Failed to draw wheel item '{}'", item.id(), e));
      }
    }
  }

  private void drawAvailabilityIndicator(
      DrawContext ctx, WheelMenuItem item, float iconX, float iconY, float iconSize) {
    switch (item.availability()) {
      case AVAILABLE -> {}
      case UNAVAILABLE ->
          AvailabilityIndicatorRenderer.drawUnavailable(ctx, iconX, iconY, iconSize);
      case UNREGISTERED ->
          AvailabilityIndicatorRenderer.drawUnregistered(ctx, iconX, iconY, iconSize);
    }
  }

  private void drawCenterInfo(
      DrawContext ctx, WheelMenu.Layout layout, WheelMenuItem item, double scale) {
    float cx = layout.center().x();
    float cy = layout.center().y();
    float centerIconSize = layout.vmin(CENTER_ICON_SIZE);

    {
      float scaledIconSize = centerIconSize * (float) scale;
      float scaledHalfIconSize = scaledIconSize / 2;

      Identifier icon = item.icon();
      if (icon != null) {
        int ix = (int) (cx - scaledHalfIconSize);
        int iy = (int) (cy - scaledHalfIconSize);
        int is = (int) scaledIconSize;
        ctx.blit(icon, 0, 0, ix, iy, is, is, is, is, 1);
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
          (int) (cy + halfCenterIconSize + font.lineHeight),
          COLOR_TEXT,
          true);
    }
  }

  private void drawDebugAnchor(DrawContext ctx, WheelMenu.Layout layout, WheelAnchor anchor) {
    final float HALF_SIZE = 1;
    final int COLOR = 0xFF_FF_FF_00;

    var center = layout.center();

    float x = center.x() + anchor.getOffsetX();
    float y = center.y() + anchor.getOffsetY();

    ctx.fill(
        (int) (x - HALF_SIZE),
        (int) (y - HALF_SIZE),
        (int) (x + HALF_SIZE),
        (int) (y + HALF_SIZE),
        COLOR);
  }
}
