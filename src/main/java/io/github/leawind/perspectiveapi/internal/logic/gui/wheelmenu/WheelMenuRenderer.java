package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

import static io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu.WheelMenuManager.ROTATE_OFFSET_RAD;

import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Renders the perspective wheel menu overlay.
///
/// Only the currently selected sector is highlighted with a white semi-transparent
/// filled ring-sector shape. Icons and center label are drawn on top.
public final class WheelMenuRenderer {
  private static final WheelMenuRenderer INSTANCE = new WheelMenuRenderer();

  private static final int COLOR_TEXT = 0xFF_FF_FF_FF;
  private static final int COLOR_AVAILABLE = 0x00_22_C5_5E;
  private static final int COLOR_UNAVAILABLE = 0xFF_EA_B3_08;
  private static final int COLOR_UNREGISTERED = 0xFF_EF_44_44;
  private static final float SELECTED_SCALE = 1.35f;

  private WheelMenuRenderer() {}

  public static @NonNull WheelMenuRenderer getInstance() {
    return INSTANCE;
  }

  public void render(DrawContext ctx, int screenWidth, int screenHeight) {
    WheelMenuManager manager = WheelMenuManager.getInstance();
    if (!manager.isVisible()) return;

    float alpha = manager.getAlphaMultiplier();

    WheelMenuManager.WheelMenuLayout layout = manager.getLayout(screenWidth, screenHeight);
    var items = manager.getItems();

    if (!items.isEmpty()) {
      int selectedIndex = manager.getSelectedIndex();

      drawRingIcons(ctx, layout, items, selectedIndex, alpha);
      drawCenterInfo(ctx, layout, items, selectedIndex, alpha);
    }

    if (WheelMenuManager.DEBUG) {
      drawAnchorDebug(ctx, layout, alpha);
    }
  }

  /// Applies an alpha multiplier to a color.
  ///
  /// @param color the base color in ARGB format (e.g., `0xFF_FF_FF_FF`).
  ///     The alpha channel must be explicitly set; colors without an alpha
  ///     component (e.g., `0xFF_FF_FF`) will be treated as fully transparent.
  /// @param alpha multiplier in `[0.0, 1.0]`
  /// @return the color with adjusted alpha, in ARGB format
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
      DrawContext ctx,
      WheelMenuManager.WheelMenuLayout layout,
      List<WheelMenuItem> items,
      int selectedIndex,
      float alpha) {
    float centerX = layout.centerX();
    float centerY = layout.centerY();
    float iconRadius = layout.iconRadius();

    double sectorRad = 2 * Math.PI / items.size();

    for (int i = 0; i < items.size(); i++) {
      WheelMenuItem item = items.get(i);
      boolean isSelected = i == selectedIndex;

      double itemRad = sectorRad * i + ROTATE_OFFSET_RAD;
      float iconX = centerX + iconRadius * (float) Math.cos(itemRad);
      float iconY = centerY + iconRadius * (float) Math.sin(itemRad);

      float iconSize = layout.iconSize();
      if (isSelected) {
        iconSize *= SELECTED_SCALE;
      }
      float halfIconSize = iconSize / 2;
      int slotSize = (int) iconSize;
      int sx = (int) (iconX - halfIconSize);
      int sy = (int) (iconY - halfIconSize);

      ctx.drawGamemodeSwitcherSlot(sx, sy, slotSize, slotSize, applyAlpha(0xFF_FF_FF_FF, alpha));
      if (isSelected) {
        ctx.drawGamemodeSwitcherSelection(
            sx, sy, slotSize, slotSize, applyAlpha(0xFF_FF_FF_FF, alpha));
      }

      drawAvailabilityIndicator(ctx, item, iconX + halfIconSize, iconY + halfIconSize, alpha);

      // Icon with 5px padding (matching GameModeSwitcherScreen's 16x16 inside 26x26)
      Identifier icon = getPerspectiveIcon(item);
      if (icon != null) {
        float pad = iconSize * 0.19f; // ~5px padding for 26px slot
        int ix = (int) (iconX - halfIconSize + pad);
        int iy = (int) (iconY - halfIconSize + pad);
        int is = (int) (iconSize - pad * 2);
        ctx.blit(icon, 0, 0, ix, iy, is, is, is, is);
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
      DrawContext ctx,
      WheelMenuManager.WheelMenuLayout layout,
      List<WheelMenuItem> items,
      int selectedIndex,
      float alpha) {
    if (items.isEmpty()) return;

    int idx = Math.floorMod(selectedIndex, items.size());
    WheelMenuItem item = items.get(idx);
    float cx = layout.centerX();
    float cy = layout.centerY();
    float centerIconSize = layout.centerIconSize();
    float halfCenterIconSize = layout.centerIconSize() / 2;

    // Icon
    {
      Identifier icon = getPerspectiveIcon(item);
      if (icon != null) {
        int ix = (int) (cx - halfCenterIconSize);
        int iy = (int) (cy - halfCenterIconSize - 6);
        int is = (int) centerIconSize;
        ctx.blit(icon, 0, 0, ix, iy, is, is, is, is);
      }
    }

    // Name and description
    {
      Minecraft minecraft = Minecraft.getInstance();
      Font font = minecraft.font;

      // Name
      {
        Component text = getDisplayName(item);
        ctx.text(
            font,
            text,
            (int) (cx - font.width(text) / 2.0f),
            (int) (cy + halfCenterIconSize + 2),
            applyAlpha(COLOR_TEXT, alpha),
            true);
      }

      // Description
      {
        Component text = getDisplayDescription(item);
        if (text != null) {
          ctx.text(
              font,
              text,
              (int) (cx - font.width(text) / 2.0f),
              (int) (cy + halfCenterIconSize + font.lineHeight * 2),
              applyAlpha(COLOR_TEXT, alpha),
              true);
        }
      }
    }
  }

  @Deprecated
  private static @NonNull Component getDisplayName(@NonNull WheelMenuItem item) {
    if (item.perspective() != null) {
      return item.perspective().getNameComponent();
    }
    return Component.literal(item.id().toString());
  }

  @Deprecated
  private static @Nullable Component getDisplayDescription(@NonNull WheelMenuItem item) {
    if (item.perspective() != null) {
      return item.perspective().getDescriptionComponent();
    }
    return Component.literal(item.id().toString());
  }

  @Deprecated
  private static @Nullable Identifier getPerspectiveIcon(@NonNull WheelMenuItem item) {
    if (item.perspective() != null) {
      return item.perspective().icon();
    }
    return null;
  }
}
