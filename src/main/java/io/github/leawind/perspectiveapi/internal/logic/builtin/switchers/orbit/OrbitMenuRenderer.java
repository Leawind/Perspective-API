package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GuiRenderContext;
import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
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
  private static final int COLOR_UNAVAILABLE = 0xFF_EA_B3_08;
  private static final int COLOR_SELECTED_AREA = 0x33_FF_FF_FF;
  private static final int COLOR_CANDIDATE_AREA = 0x22_3B_82_F6;
  private static final int COLOR_DISABLED_AREA = 0x22_EF_44_44;

  void render(GuiRenderContext context, OrbitMenu menu) {
    DrawContext canvas = context.drawContext;
    if (menu.mode() == OrbitMenu.Mode.EDITING) drawEditingAreas(canvas, context, menu);

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

  private void drawEditingAreas(DrawContext canvas, GuiRenderContext context, OrbitMenu menu) {
    int width = context.screenWidth;
    int height = context.screenHeight;
    int leftEnd = (int) menu.worldToScreenX(-0.27);
    int rightStart = (int) menu.worldToScreenX(0.27);
    canvas.fill(0, 0, leftEnd, height, COLOR_CANDIDATE_AREA);
    canvas.fill(leftEnd, 0, rightStart, height, COLOR_SELECTED_AREA);
    canvas.fill(rightStart, 0, width, height, COLOR_DISABLED_AREA);

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

  private void drawActor(
      DrawContext canvas, OrbitMenu menu, PerspectiveActor actor, double renderScale) {
    Perspective perspective = menu.model().perspective(actor.perspectiveId());
    if (perspective == null) return;

    double minEdge =
        Math.min(
            Math.abs(menu.worldToScreenX(1) - menu.worldToScreenX(0)),
            Math.abs(menu.worldToScreenY(1) - menu.worldToScreenY(0)));
    int size = Math.max((int) (minEdge * OrbitMenu.ICON_SIZE * renderScale), 8);
    int x = (int) menu.worldToScreenX(actor.body().position().x) - size / 2;
    int y = (int) menu.worldToScreenY(actor.body().position().y) - size / 2;

    canvas.drawGamemodeSwitcherSlot(x, y, size, size, COLOR_TEXT);
    if (actor.perspectiveId().equals(menu.model().resolvedId())) {
      canvas.drawGamemodeSwitcherSelection(x, y, size, size, COLOR_TEXT);
    }

    Identifier icon = perspective.icon();
    if (icon == null) icon = DEFAULT_ICON;
    int padding = Math.max((int) (size * 0.19), 1);
    int iconSize = size - padding * 2;
    canvas.blit(icon, 0, 0, x + padding, y + padding, iconSize, iconSize, iconSize, iconSize, 1);

    if (!perspective.isAvailable()) {
      int indicatorSize = Math.max(size / 5, 3);
      canvas.fill(
          x + size - indicatorSize,
          y + size - indicatorSize,
          x + size,
          y + size,
          COLOR_UNAVAILABLE);
    }
  }

  private void drawLabel(
      DrawContext canvas, GuiRenderContext context, OrbitMenu menu, PerspectiveActor actor) {
    Perspective perspective = menu.model().perspective(actor.perspectiveId());
    if (perspective == null) return;
    drawCenteredText(
        canvas, perspective.name(), context.screenWidth / 2, context.screenHeight / 2 + 54);
  }

  private void drawCenteredText(DrawContext canvas, Component text, int centerX, int y) {
    Font font = Minecraft.getInstance().font;
    canvas.text(font, text, centerX - font.width(text) / 2, y, COLOR_TEXT, true);
  }
}
