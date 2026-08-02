package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GuiRenderContext;
import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.AvailabilityIndicatorRenderer;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.OrbitSwitcherModel.Group;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class OrbitMenuRenderer {
  private static final Identifier DEFAULT_ICON =
      Bridge.parseIdentifier("perspective_api:textures/perspective/default.png");

  private static final int COLOR_TEXT = 0xFF_FF_FF_FF;
  private static final int COLOR_ORBIT = 0x99_FF_FF_FF;
  private static final int COLOR_GHOST_SLOT = 0x66_FF_FF_FF;
  private static final double ORBIT_DASH_PERIOD_PX = 14;
  private static final double ORBIT_DASH_RATIO = 0.57;

  void render(GuiRenderContext context, OrbitMenu menu) {
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
      if (actor != grabbed && actor != hovered) drawActor(canvas, menu, actor, 1);
    }
    if (hovered != null && hovered != grabbed) drawActor(canvas, menu, hovered, 1.15);
    if (grabbed != null) drawActor(canvas, menu, grabbed, 1.22);

    PerspectiveActor labelActor = grabbed != null ? grabbed : hovered;
    if (labelActor != null) drawLabel(canvas, context, menu, labelActor);
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
    canvas.drawGamemodeSwitcherSlot(x, y, size, size, COLOR_GHOST_SLOT);
  }

  private void drawEditingAreas(DrawContext canvas, GuiRenderContext context, OrbitMenu menu) {
    int width = context.screenWidth;
    int leftEnd = (int) menu.worldToScreenX(-0.27);
    int rightStart = (int) menu.worldToScreenX(0.27);
    drawDashedOrbit(canvas, menu);

    drawCenteredText(
        canvas,
        Component.translatable("perspective_api.switcher.orbit_switcher.candidate"),
        leftEnd / 2,
        12);
    drawCenteredText(
        canvas,
        Component.translatable("perspective_api.switcher.orbit_switcher.selected"),
        (leftEnd + rightStart) / 2,
        12);
    drawCenteredText(
        canvas,
        Component.translatable("perspective_api.switcher.orbit_switcher.disabled"),
        (rightStart + width) / 2,
        12);
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

  private void drawActor(
      DrawContext canvas, OrbitMenu menu, PerspectiveActor actor, double renderScale) {
    Perspective perspective = menu.model().perspective(actor.perspectiveId());
    if (perspective == null) return;

    int size = actorSize(menu, renderScale);
    int x = (int) menu.worldToScreenX(actor.body().position().x) - size / 2;
    int y = (int) menu.worldToScreenY(actor.body().position().y) - size / 2;

    canvas.drawGamemodeSwitcherSlot(x, y, size, size, COLOR_TEXT);
    if (actor.perspectiveId().equals(menu.model().resolvedId())) {
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
