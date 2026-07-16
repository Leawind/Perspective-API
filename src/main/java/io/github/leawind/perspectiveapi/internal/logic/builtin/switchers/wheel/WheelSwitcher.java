package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.wheel;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.spi.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.utils.KeyStateTracker;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class WheelSwitcher implements PerspectiveSwitcher {
  private final PerspectiveRegistry registry;

  private volatile List<String> list = new ArrayList<>();
  private volatile @Nullable String selected = null;

  private final WheelMenu wheelMenu = new WheelMenu();

  public WheelSwitcher(PerspectiveRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void init() {
    GameClientEvents.HANDLE_KEYBINDS_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()) return;

          KeyStateTracker.of(
                  "perspective_api.wheel_switcher",
                  minecraft.options.keyTogglePerspective,
                  builder ->
                      builder
                          .setHoldTicks(3)
                          .onPress(this::cycleForward)
                          .onHoldStart(this::openWheel)
                          .onHoldStop(this::closeWheel))
              .tick()
              .drain();
        });
    GameClientEvents.RENDER_GUI_OVERLAY.on(
        ctx -> {
          if (!PerspectiveAPI.isEnabled()) return;
          var minecraft = Minecraft.getInstance();
          if (minecraft.level == null) return;

          if (wheelMenu.isOpened() || wheelMenu.isAnimating()) {
            wheelMenu.render(ctx.drawContext, ctx.screenWidth, ctx.screenHeight);
          }
        });
    GameClientEvents.MOUSE_INPUT.on(
        ctx -> {
          if (!PerspectiveAPI.isEnabled()) return;
          if (!wheelMenu.isOpened()) return;

          switch (ctx.type) {
            case SCROLL -> wheelMenu.onMouseScroll(ctx.scrollDelta);
            case MOVE -> wheelMenu.onMouseMove(ctx.mouseX, -ctx.mouseY);
            case BUTTON -> {}
          }
          // Always consume input when the wheel menu is open
          ctx.consumed = true;
        });
  }

  @Override
  public void onUpdateSwitchables(@NonNull List<Perspective> switchables) {
    this.list = new ArrayList<>(switchables.stream().map(Perspective::id).toList());
  }

  @Override
  public @NonNull Component getNameComponent() {
    return Component.translatable("perspective_api.switcher.wheel_switcher.name");
  }

  @Override
  public @NonNull Component getDescriptionComponent() {
    return Component.translatable("perspective_api.switcher.wheel_switcher.description");
  }

  @Override
  public void onActivated(@NonNull Perspective currentPerspective) {
    if (list.contains(currentPerspective.id())) {
      this.selected = currentPerspective.id();
    }
  }

  @Override
  public void clientTickWhenActive() {
    wheelMenu.tick();

    String selected = this.selected;
    if (selected != null) {
      var p = registry.get(selected);
      if (p == null || !p.isAvailable()) {
        cycleBackward();
      }
    }
  }

  @Override
  public void onDeactivated() {
    if (wheelMenu.isOpened()) {
      String finalId = wheelMenu.close();
      if (finalId != null) this.selected = finalId;
    }
  }

  @Override
  public @Nullable String getSelected() {
    var selected = this.selected;
    if (selected == null) {
      String current = PerspectiveAPI.getCurrent().id();
      if (list.contains(current)) {
        this.selected = current;
      }
    }
    return this.selected;
  }

  // region wheel menu

  private void openWheel() {
    if (!PerspectiveAPI.isEnabled()) return;
    var minecraft = Minecraft.getInstance();
    if (minecraft.level == null || minecraft.player == null) return;
    if (PerspectiveAPI.getCurrentSwitcher() != this) return;

    wheelMenu.setOnHover(id -> this.selected = id);
    wheelMenu.open(getSelected(), this.list);
  }

  private void closeWheel() {
    String finalId = wheelMenu.close();
    wheelMenu.setOnHover(null);
    if (finalId != null) {
      this.selected = finalId;
    }
  }

  // endregion

  // region cycling

  /// Advances the active perspective to the next available one.
  private synchronized void cycleForward() {
    if (list.isEmpty()) return;

    String current = selected;
    int idx = list.indexOf(current);
    int start = idx < 0 ? 0 : (idx + 1) % list.size();

    int attempts = 0;
    int size = list.size();
    int i = start;
    do {
      String next = list.get(i);
      var p = registry.get(next);
      if (p != null && p.isAvailable()) {
        selected = next;
        return;
      }
      i = (i + 1) % size;
      attempts++;
    } while (attempts < size);
  }

  /// Moves the active perspective to the previous available one.
  private synchronized void cycleBackward() {
    if (list.isEmpty()) return;

    String current = selected;
    int idx = list.indexOf(current);
    int size = list.size();
    int i = idx < 0 ? size - 1 : (idx - 1 + size) % size;

    int attempts = 0;
    do {
      String next = list.get(i);

      var p = registry.get(next);
      if (p != null && p.isAvailable()) {
        selected = next;
        return;
      }
      i = (i - 1 + size) % size;
      attempts++;
    } while (attempts < size);
  }

  // endregion
}
