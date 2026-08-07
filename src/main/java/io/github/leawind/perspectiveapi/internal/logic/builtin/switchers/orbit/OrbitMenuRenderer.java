package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GuiRenderContext;
import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.AvailabilityIndicatorRenderer;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.OrbitSwitcherModel.Group;
import io.github.leawind.perspectiveapi.internal.utils.smooth.ExpSmoothDouble;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class OrbitMenuRenderer {
  static final String SPONSOR_TEXT_KEY = "perspective_api.switcher.orbit_switcher.sponsor";

  private static final Identifier DEFAULT_ICON =
      Bridge.parseIdentifier("perspective_api:textures/perspective/default.png");

  private static final Component HELP_BUTTON_TEXT = Component.literal("?");
  private static final int COLOR_TEXT = 0xFF_FF_FF_FF;
  private static final int COLOR_ORBIT = 0x99_FF_FF_FF;
  private static final int COLOR_GHOST_SLOT = 0xCC_FF_FF_FF;
  private static final int COLOR_SPONSOR_TEXT = 0x55_FF_FF_FF;
  private static final int COLOR_HELP_BACKGROUND = 0xDD_20_20_20;
  private static final double ORBIT_DASH_PERIOD_PX = 14;
  private static final double ORBIT_DASH_RATIO = 0.57;
  private static final int GHOST_DASH_PERIOD_PX = 8;
  private static final int GHOST_DASH_LENGTH_PX = 4;
  private static final int EDITING_TITLE_Y = 12;
  private static final int HELP_BUTTON_SIZE = 11;
  private static final int HELP_BUTTON_MARGIN = 3;
  private static final String EDITING_HELP_KEY = "perspective_api.switcher.orbit_switcher.help";
  private static final double SCALE_HALFLIFE = 0.008;
  private static final double HOVER_SCALE = 1.15;
  private static final double GRAB_SCALE = 1.22;
  private final Map<String, ExpSmoothDouble> smoothScales = new HashMap<>();

  void render(GuiRenderContext context, OrbitMenu menu, double frameSeconds) {
    DrawContext canvas = context.drawContext;
    if (menu.mode() == OrbitMenu.Mode.EDITING) drawEditingAreas(canvas, context, menu);
    drawGrabbedWheelSlot(canvas, menu);

    List<PerspectiveActor> actors = new ArrayList<>();
    for (PerspectiveActor actor : menu.actors()) {
      if (menu.mode() == OrbitMenu.Mode.EDITING
          || menu.model().groupOf(actor.perspectiveId()) == Group.SELECTED) {
        actors.add(actor);
      }
    }
    PerspectiveActor grabbed = menu.grabbedActor();
    PerspectiveActor hovered = menu.hoveredActor();
    for (PerspectiveActor actor : actors) {
      smoothScales
          .computeIfAbsent(
              actor.perspectiveId(),
              ignored -> new ExpSmoothDouble().setHalflife(SCALE_HALFLIFE).setCurrent(1))
          .setTarget(targetScale(menu, actor))
          .update(frameSeconds);
    }
    for (PerspectiveActor actor : actors) {
      if (actor != grabbed && actor != hovered) drawActor(canvas, menu, actor);
    }
    if (hovered != null && hovered != grabbed) drawActor(canvas, menu, hovered);
    if (grabbed != null) drawActor(canvas, menu, grabbed);

    PerspectiveActor labelActor;
    if (menu.mode() == OrbitMenu.Mode.SELECTING) {
      String resolvedId = menu.model().resolvedId();
      labelActor = resolvedId != null ? menu.actor(resolvedId) : null;
    } else {
      labelActor = null;
    }

    if (labelActor != null) drawLabel(canvas, context, menu, labelActor);
    if (menu.mode() == OrbitMenu.Mode.EDITING) {
      drawSponsorButton(canvas, menu);
      drawEditingHelpTooltip(canvas, menu);
      if (hovered != null && hovered != grabbed) drawActorTooltip(canvas, menu, hovered);
    }
  }

  private void drawSponsorButton(DrawContext canvas, OrbitMenu menu) {
    EvasiveSponsorButton button = menu.sponsorButton();
    if (!button.isVisible()) return;

    int x = button.x();
    int y = button.y();
    int width = button.width();
    Font font = Minecraft.getInstance().font;
    Component text = Component.translatable(SPONSOR_TEXT_KEY);
    canvas.text(
        font,
        text,
        x + (width - font.width(text)) / 2,
        y + (button.height() - font.lineHeight) / 2,
        COLOR_SPONSOR_TEXT,
        true);
  }

  private void drawGrabbedWheelSlot(DrawContext canvas, OrbitMenu menu) {
    PerspectiveActor grabbed = menu.grabbedActor();
    if (grabbed == null || menu.model().groupOf(grabbed.perspectiveId()) != Group.SELECTED) return;

    List<String> selected = menu.model().selected();
    int index = selected.indexOf(grabbed.perspectiveId());
    if (index < 0 || selected.isEmpty()) return;

    double angleRad = Math.PI * 2 * index / selected.size() - Math.PI / 2;
    int size = actorSize(menu, 1);
    int x = (int) menu.worldToScreenX(OrbitMenu.RING_RADIUS * Math.cos(angleRad)) - size / 2;
    int y = (int) menu.worldToScreenY(OrbitMenu.RING_RADIUS * Math.sin(angleRad)) - size / 2;
    drawDashedRoundedSquare(canvas, x, y, size);
  }

  private void drawDashedRoundedSquare(DrawContext canvas, int x, int y, int size) {
    int right = x + size - 1;
    int bottom = y + size - 1;
    int radius = Math.max(Math.min(size / 5, (size - 1) / 2), 2);

    drawDashedHorizontal(canvas, x + radius, right - radius, y);
    drawDashedHorizontal(canvas, x + radius, right - radius, bottom);
    drawDashedVertical(canvas, y + radius, bottom - radius, x);
    drawDashedVertical(canvas, y + radius, bottom - radius, right);

    drawDashedArc(canvas, x + radius, y + radius, radius, Math.PI, Math.PI * 1.5);
    drawDashedArc(canvas, right - radius, y + radius, radius, Math.PI * 1.5, Math.PI * 2);
    drawDashedArc(canvas, right - radius, bottom - radius, radius, 0, Math.PI * 0.5);
    drawDashedArc(canvas, x + radius, bottom - radius, radius, Math.PI * 0.5, Math.PI);
  }

  private void drawDashedHorizontal(DrawContext canvas, int startX, int endX, int y) {
    for (int x = startX; x <= endX; x += GHOST_DASH_PERIOD_PX) {
      canvas.fill(x, y - 1, Math.min(x + GHOST_DASH_LENGTH_PX, endX + 1), y + 1, COLOR_GHOST_SLOT);
    }
  }

  private void drawDashedVertical(DrawContext canvas, int startY, int endY, int x) {
    for (int y = startY; y <= endY; y += GHOST_DASH_PERIOD_PX) {
      canvas.fill(x - 1, y, x + 1, Math.min(y + GHOST_DASH_LENGTH_PX, endY + 1), COLOR_GHOST_SLOT);
    }
  }

  private void drawDashedArc(
      DrawContext canvas, int centerX, int centerY, int radius, double startRad, double endRad) {
    int samples = Math.max((int) Math.ceil(radius * (endRad - startRad)), 1);
    for (int sample = 0; sample <= samples; sample++) {
      if (sample % GHOST_DASH_PERIOD_PX >= GHOST_DASH_LENGTH_PX) continue;
      double angleRad = startRad + (endRad - startRad) * sample / samples;
      int x = (int) Math.round(centerX + radius * Math.cos(angleRad));
      int y = (int) Math.round(centerY + radius * Math.sin(angleRad));
      canvas.fill(x - 1, y - 1, x + 1, y + 1, COLOR_GHOST_SLOT);
    }
  }

  private void drawEditingAreas(DrawContext canvas, GuiRenderContext context, OrbitMenu menu) {
    int width = context.screenWidth;
    int leftEnd = (int) menu.worldToScreenX(-0.27);
    int rightStart = (int) menu.worldToScreenX(0.27);
    int selectedCenterX = (int) menu.worldToScreenX(0);
    Component selectedTitle =
        Component.translatable("perspective_api.switcher.orbit_switcher.selected");
    drawDashedOrbit(canvas, menu);

    drawCenteredText(
        canvas,
        Component.translatable("perspective_api.switcher.orbit_switcher.candidate"),
        leftEnd / 2,
        EDITING_TITLE_Y);
    drawCenteredText(canvas, selectedTitle, selectedCenterX, EDITING_TITLE_Y);
    drawEditingHelpButton(canvas, menu, selectedCenterX, selectedTitle);
    drawCenteredText(
        canvas,
        Component.translatable("perspective_api.switcher.orbit_switcher.disabled"),
        (rightStart + width) / 2,
        EDITING_TITLE_Y);
  }

  private void drawEditingHelpButton(
      DrawContext canvas, OrbitMenu menu, int selectedCenterX, Component selectedTitle) {
    Font font = Minecraft.getInstance().font;
    int x = helpButtonX(font, selectedCenterX, selectedTitle);
    int y = helpButtonY(font);

    canvas.fill(x, y, x + HELP_BUTTON_SIZE, y + HELP_BUTTON_SIZE, COLOR_TEXT);
    canvas.fill(
        x + 1, y + 1, x + HELP_BUTTON_SIZE - 1, y + HELP_BUTTON_SIZE - 1, COLOR_HELP_BACKGROUND);
    canvas.text(
        font,
        HELP_BUTTON_TEXT,
        x + (HELP_BUTTON_SIZE - font.width(HELP_BUTTON_TEXT)) / 2,
        y + (HELP_BUTTON_SIZE - font.lineHeight) / 2,
        COLOR_TEXT,
        true);
  }

  private void drawEditingHelpTooltip(DrawContext canvas, OrbitMenu menu) {
    Font font = Minecraft.getInstance().font;
    if (!isEditingHelpButtonHovered(menu)) return;

    String[] lineTexts = Component.translatable(EDITING_HELP_KEY).getString().split("\\n");
    List<Component> lines = new ArrayList<>(lineTexts.length);
    for (String lineText : lineTexts) {
      lines.add(Component.literal(lineText));
    }
    canvas.tooltip(font, lines, menu.mouseScreenX(), menu.mouseScreenY());
  }

  private void drawActorTooltip(DrawContext canvas, OrbitMenu menu, PerspectiveActor actor) {
    Perspective perspective = menu.model().perspective(actor.perspectiveId());
    if (perspective == null) return;

    List<Component> lines = new ArrayList<>();
    lines.add(perspective.info().name());
    Component description = perspective.info().description();
    if (description != null) {
      for (String lineText : description.getString().split("\\n")) {
        lines.add(Component.literal(lineText));
      }
    }
    canvas.tooltip(Minecraft.getInstance().font, lines, menu.mouseScreenX(), menu.mouseScreenY());
  }

  private int helpButtonX(Font font, int selectedCenterX, Component selectedTitle) {
    return selectedCenterX + font.width(selectedTitle) / 2 + HELP_BUTTON_MARGIN;
  }

  private int helpButtonY(Font font) {
    return EDITING_TITLE_Y - (HELP_BUTTON_SIZE - font.lineHeight) / 2;
  }

  boolean isEditingHelpButtonHovered(OrbitMenu menu) {
    Font font = Minecraft.getInstance().font;
    Component selectedTitle =
        Component.translatable("perspective_api.switcher.orbit_switcher.selected");
    return menu.isMouseOver(
        helpButtonX(font, (int) menu.worldToScreenX(0), selectedTitle),
        helpButtonY(font),
        HELP_BUTTON_SIZE,
        HELP_BUTTON_SIZE);
  }

  private void drawDashedOrbit(DrawContext canvas, OrbitMenu menu) {
    double centerX = menu.worldToScreenX(0);
    double centerY = menu.worldToScreenY(0);
    double radius = Math.abs(menu.worldToScreenX(OrbitMenu.RING_RADIUS) - centerX);
    if (radius < 1) return;

    double circumference = Math.PI * 2 * radius;
    int dashCount = Math.max((int) Math.round(circumference / ORBIT_DASH_PERIOD_PX), 12);
    double periodRad = Math.PI * 2 / dashCount;
    double dashRad = periodRad * ORBIT_DASH_RATIO;
    int samplesPerDash = Math.max((int) Math.ceil(radius * dashRad / 2), 2);

    for (int dash = 0; dash < dashCount; dash++) {
      double startRad = dash * periodRad;
      for (int sample = 0; sample <= samplesPerDash; sample++) {
        double angleRad = startRad + dashRad * sample / samplesPerDash;
        int x = (int) Math.round(centerX + radius * Math.cos(angleRad));
        int y = (int) Math.round(centerY + radius * Math.sin(angleRad));
        canvas.fill(x - 1, y - 1, x + 1, y + 1, COLOR_ORBIT);
      }
    }
  }

  private static double targetScale(OrbitMenu menu, PerspectiveActor actor) {
    if (actor == menu.grabbedActor()) return GRAB_SCALE;
    if (menu.mode() == OrbitMenu.Mode.SELECTING
        && actor.perspectiveId().equals(menu.model().resolvedId())) {
      return HOVER_SCALE;
    }
    return 1;
  }

  private void drawActor(DrawContext canvas, OrbitMenu menu, PerspectiveActor actor) {
    Perspective perspective = menu.model().perspective(actor.perspectiveId());
    if (perspective == null) return;

    double renderScale = smoothScales.get(actor.perspectiveId()).getCurrent();
    int size = actorSize(menu, renderScale);
    int x = (int) menu.worldToScreenX(actor.body().position().x) - size / 2;
    int y = (int) menu.worldToScreenY(actor.body().position().y) - size / 2;

    canvas.drawGamemodeSwitcherSlot(x, y, size, size, COLOR_TEXT);
    if (actor.perspectiveId().equals(menu.model().activeId())) {
      canvas.drawGamemodeSwitcherSelection(x, y, size, size, COLOR_TEXT);
    }

    Identifier icon = perspective.info().icon();
    if (icon == null) icon = DEFAULT_ICON;
    int padding = Math.max((int) (size * 0.19), 1);
    int iconSize = size - padding * 2;
    canvas.blit(icon, 0, 0, x + padding, y + padding, iconSize, iconSize, iconSize, iconSize, 1);

    if (!perspective.isAvailable()) {
      AvailabilityIndicatorRenderer.drawUnavailable(canvas, x + size * 0.5f, y + size * 0.5f, size);
    }
  }

  private int actorSize(OrbitMenu menu, double renderScale) {
    double minEdge =
        Math.min(
            Math.abs(menu.worldToScreenX(1) - menu.worldToScreenX(0)),
            Math.abs(menu.worldToScreenY(1) - menu.worldToScreenY(0)));
    return Math.max((int) (minEdge * OrbitMenu.ICON_SIZE * renderScale), 8);
  }

  private void drawLabel(
      DrawContext canvas, GuiRenderContext context, OrbitMenu menu, PerspectiveActor actor) {
    Perspective perspective = menu.model().perspective(actor.perspectiveId());
    if (perspective == null) return;
    drawCenteredText(
        canvas, perspective.info().name(), context.screenWidth / 2, context.screenHeight / 2 + 54);
  }

  private void drawCenteredText(DrawContext canvas, Component text, int centerX, int y) {
    Font font = Minecraft.getInstance().font;
    canvas.text(font, text, centerX - font.width(text) / 2, y, COLOR_TEXT, true);
  }
}
