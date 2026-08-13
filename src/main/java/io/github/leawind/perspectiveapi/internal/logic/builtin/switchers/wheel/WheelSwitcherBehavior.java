package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.wheel;

import com.google.auto.service.AutoService;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.utils.KeyStateTracker;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(PerspectiveSwitcherBehavior.class)
public class WheelSwitcherBehavior implements PerspectiveSwitcherBehavior {
  private static final Logger LOGGER = LoggerFactory.getLogger(WheelSwitcherBehavior.class);
  public static final String ID = PerspectiveAPI.MOD_ID + ".wheel_switcher";

  private @Nullable KeyStateTracker keyStateTracker;
  private final PerspectiveRegistry registry;

  private List<String> list = new ArrayList<>();

  private final WheelMenu wheelMenu = new WheelMenu();

  public WheelSwitcherBehavior() {
    this(PerspectiveAPI.getRegistry());
  }

  public WheelSwitcherBehavior(PerspectiveRegistry registry) {
    this.registry = registry;
  }

  public @NonNull KeyStateTracker getKeyStateTracker() {
    if (keyStateTracker == null) {
      keyStateTracker =
          KeyStateTracker.builder()
              .setHoldTicks(3)
              .onPress(this::cycleForward)
              .onHoldStart(this::openWheel)
              .onHoldStop(this::closeWheel)
              .build();
    }
    return keyStateTracker;
  }

  @Override
  public void init() {
    GameClientEvents.HANDLE_KEYBINDS_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()) return;
          if (PerspectiveAPI.getSwitcherManager().getSelectedSwitcher() != this) return;

          var key = minecraft.options.keyTogglePerspective;
          getKeyStateTracker().tick(key.isDown());
          while (key.consumeClick()) {}
        });
    GameClientEvents.CLIENT_TICK_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()
              || minecraft.level == null
              || minecraft.player == null
              || PerspectiveAPI.getSwitcherManager().getSelectedSwitcher() != this) return;

          wheelMenu.tick();
          if (wheelMenu.isOpened() && Bridge.getScreen(minecraft) != null) closeWheel();
        });
    GameClientEvents.RENDER_GUI_OVERLAY.on(
        ctx -> {
          if (!PerspectiveAPI.isEnabled()) return;
          var minecraft = Minecraft.getInstance();
          if (minecraft.level == null) return;

          wheelMenu.render(ctx.drawContext, ctx.screenWidth, ctx.screenHeight);
        });
    GameClientEvents.MOUSE_INPUT.on(
        ctx -> {
          if (!PerspectiveAPI.isEnabled()) return;
          if (!wheelMenu.isOpened()) return;

          switch (ctx.type) {
            case SCROLL -> wheelMenu.onMouseScroll(ctx.scrollDelta);
            case MOVE -> wheelMenu.onMouseMove(ctx.mouseX, ctx.mouseY);
            case BUTTON -> {}
          }
          // Always consume input when the wheel menu is open
          ctx.consumed = true;
        });
  }

  @Override
  public @NonNull String id() {
    return ID;
  }

  @Override
  public void onSwitchablePerspectivesUpdated(
      @NonNull List<@NonNull Perspective> switchablePerspectives) {
    list =
        switchablePerspectives.stream()
            .map(perspective -> perspective.info().id())
            .collect(Collectors.toCollection(ArrayList::new));
    LOGGER.info("Switchable perspective list updated: {}", list);
    wheelMenu.updateItems(list);
  }

  @Override
  public @NonNull Component name() {
    return Component.translatable("perspective_api.switcher.wheel_switcher.name");
  }

  @Override
  public @NonNull Component description() {
    return Component.translatable("perspective_api.switcher.wheel_switcher.description");
  }

  @Override
  public void onDeactivated() {
    getKeyStateTracker().reset();
    if (wheelMenu.isOpened()) {
      String finalId = wheelMenu.close();
      if (finalId != null) select(finalId);
    }
  }

  private void openWheel() {
    if (!PerspectiveAPI.isEnabled()) return;
    if (PerspectiveAPI.getSwitcherManager().getSelectedSwitcher() != this) return;
    var minecraft = Minecraft.getInstance();
    if (minecraft.level == null || minecraft.player == null) return;

    wheelMenu.setOnHover(this::select);
    wheelMenu.open(PerspectiveAPI.getSelection().selectedPerspectiveId());
  }

  private void closeWheel() {
    String finalId = wheelMenu.close();
    wheelMenu.setOnHover(null);
    if (finalId != null) {
      select(finalId);
    }
  }

  /// Advances the active perspective to the next available one.
  private void cycleForward() {
    if (list.isEmpty()) return;

    String current = PerspectiveAPI.getSelection().selectedPerspectiveId();
    int idx = list.indexOf(current);
    int start = idx < 0 ? 0 : (idx + 1) % list.size();

    int attempts = 0;
    int size = list.size();
    int i = start;
    do {
      String next = list.get(i);
      var p = registry.get(next);
      if (p != null && p.info().switchable() && p.isAvailable()) {
        PerspectiveAPI.getSelection().setSelectedPerspective(p);
        return;
      }
      i = (i + 1) % size;
      attempts++;
    } while (attempts < size);
  }

  private void select(@NonNull String perspectiveId) {
    Perspective perspective = registry.get(perspectiveId);
    if (perspective != null && perspective.info().switchable() && perspective.isAvailable()) {
      PerspectiveAPI.getSelection().setSelectedPerspective(perspective);
    }
  }
}
