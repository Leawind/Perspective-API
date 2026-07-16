package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.wheel;

import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;
import io.github.leawind.perspectiveapi.internal.utils.WheelAnchor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.joml.Vector2d;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Manages the lifecycle, state, input, and rendering of the perspective
/// wheel menu overlay as an internal component of {@link WheelSwitcherBehavior}.
public final class WheelMenu {
  private static final int MAX_ITEMS = 9;

  private boolean isOpened;
  private final WheelAnchor anchor = new WheelAnchor(24.0f);
  private final Vector2d lastMouse = new Vector2d(0, 0);
  private boolean hasLastMouse;
  private final List<WheelMenuItem> items = new ArrayList<>();
  private @Nullable String originalSelectedId;
  private @Nullable String currentHoveredId;
  private boolean moved;
  private final WheelMenuRenderer renderer = new WheelMenuRenderer();
  private @Nullable Consumer<@NonNull String> onHover;

  /// Returns whether the menu is currently open.
  public boolean isOpened() {
    return isOpened;
  }

  /// Returns whether the menu is currently playing a close animation.
  public boolean isAnimating() {
    return renderer.isAnimating();
  }

  /// Sets the callback invoked when the hovered perspective changes.
  public void setOnHover(@Nullable Consumer<@NonNull String> onHover) {
    this.onHover = onHover;
  }

  /// Opens the menu with the given current selection and available perspective
  /// IDs.
  public void open(@Nullable String currentSelectedId, @NonNull List<String> availableIds) {
    isOpened = true;
    anchor.reset();
    hasLastMouse = false;
    moved = false;
    originalSelectedId = currentSelectedId;
    currentHoveredId = currentSelectedId;

    items.clear();
    int limit = Math.min(availableIds.size(), MAX_ITEMS);
    for (int i = 0; i < limit; i++) {
      String id = availableIds.get(i);
      items.add(new WheelMenuItem(id).update());
    }

    renderer.onOpen();
  }

  /// Closes the menu and returns the perspective ID that should become active.
  /// Returns `null` if the menu was not open.
  public @Nullable String close() {
    if (!isOpened) return null;

    isOpened = false;
    renderer.onClose();

    if (!moved && originalSelectedId != null) {
      return originalSelectedId;
    }
    return currentHoveredId;
  }

  /// Ticks the menu to refresh item availability states.
  public void tick() {
    for (WheelMenuItem item : items) {
      item.update();
    }
  }

  /// Handles scroll input to rotate the item list.
  public void onMouseScroll(double vertical) {
    if (!isOpened || items.isEmpty()) return;

    boolean clockwise = vertical < 0;

    synchronized (this) {
      if (clockwise) {
        items.add(0, items.remove(items.size() - 1));
      } else {
        items.add(items.remove(0));
      }

      anchor.reset();

      double sectorRad = 2 * Math.PI / items.size();
      renderer.notifyScroll(sectorRad, clockwise);
    }
  }

  /// Handles mouse movement to update the anchor and derive the hovered
  /// sector.
  public void onMouseMove(double mouseX, double mouseY) {
    if (!isOpened) return;

    if (!hasLastMouse) {
      lastMouse.set(mouseX, mouseY);
      hasLastMouse = true;
      return;
    }

    double dx = mouseX - lastMouse.x();
    double dy = mouseY - lastMouse.y();
    lastMouse.set(mouseX, mouseY);
    anchor.moveBy((float) dx, (float) dy);

    if (anchor.isOnBorder() && !items.isEmpty()) {
      int id =
          WheelMenuUtils.getSectorIndex(
              items.size(), WheelMenuRenderer.ROTATE_OFFSET_RAD, anchor.getAngleRad());

      if (id >= 0 && id < items.size()) {
        WheelMenuItem item = items.get(id);
        if (!item.id().equals(currentHoveredId) && onHover != null) {
          onHover.accept(item.id());
        }
        currentHoveredId = item.id();
        moved = true;
      }
    }
  }

  /// Renders the menu overlay.
  public void render(DrawContext ctx, int screenWidth, int screenHeight) {
    renderer.render(ctx, screenWidth, screenHeight, items, currentHoveredId);
  }
}
