package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveWheelImpl;
import io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu.WheelMenuManager;
import io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu.WheelMenuRenderer;
import io.github.leawind.perspectiveapi.internal.logic.state.StateManagerImpl;
import io.github.leawind.perspectiveapi.internal.utils.KeyStateTracker;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Registers and handles mod event listeners.
public final class ModEvents {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModEvents.class);

  /// Registers all event handlers for client tick, keybinds, camera setup, and FOV modification.
  public static void register() {
    PerspectiveManager manager = PerspectiveManager.INSTANCE;

    GameClientEvents.CLIENT_TICK_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()) return;
          if (minecraft.level == null || minecraft.player == null) return;

          manager.clientTick(minecraft);
        });

    // region gui & input events

    GameClientEvents.HANDLE_KEYBINDS_START.on(
        (minecraft) -> {
          if (!PerspectiveAPI.isEnabled()) return;

          // Refer to Minecraft 26.2 KeyboardHandler#handleDebugKeys
          //
          // ```java
          // if (options.keyDebugSwitchGameMode.matches(event)
          //     && this.minecraft.level != null
          //     && this.minecraft.gui.screen() == null) {
          //   if (this.minecraft.canSwitchGameMode()
          //       && GameModeCommand.PERMISSION_CHECK.check(this.minecraft.player.permissions())) {
          //     this.minecraft.gui.setScreen(new GameModeSwitcherScreen());
          //   } else {
          //     this.debugFeedbackTranslated("debug.gamemodes.error");
          //   }
          //   debugAction = true;
          // }
          // ```
          KeyStateTracker.of(
                  minecraft.options.keyTogglePerspective,
                  builder ->
                      builder
                          .setHoldTicks(3)
                          .onPress(() -> manager.wheel().cycleForward())
                          .onHoldStart(() -> WheelMenuManager.getInstance().open())
                          .onHoldStop(() -> WheelMenuManager.getInstance().closeWithSelection()))
              .tick()
              .drain();
        });

    GameClientEvents.RENDER_GUI_OVERLAY.on(
        ctx -> {
          if (!PerspectiveAPI.isEnabled()) return;
          WheelMenuRenderer.getInstance()
              .render(ctx.drawContext, ctx.screenWidth, ctx.screenHeight);
        });

    GameClientEvents.MOUSE_INPUT.on(
        ctx -> {
          if (!PerspectiveAPI.isEnabled()) return;
          WheelMenuManager wmm = WheelMenuManager.getInstance();
          if (!wmm.isVisible()) return;

          switch (ctx.type) {
            case BUTTON -> wmm.onMouseButton(ctx.button, ctx.action);
            case SCROLL -> wmm.onMouseScroll(ctx.scrollDelta);
            case MOVE -> wmm.onMouseMove(ctx.mouseX, ctx.mouseY);
          }
          // Always consume input when the wheel menu is open
          ctx.consumed = true;
        });

    // endregion

    GameClientEvents.AFTER_CLIENT_LEVEL_CHANGE.on(
        ignored -> {
          if (!PerspectiveAPI.isEnabled()) return;
          manager.overrides().clearExcept(PerspectiveWheelImpl.KEY);
        });

    // region camera

    GameClientEvents.SETUP_CAMERA.on(
        (ctx) -> {
          if (!PerspectiveAPI.isEnabled()) return;
          manager.updateCamera(ctx.partialTicks, ctx.camera);
        });

    GameClientEvents.MODIFY_FIELD_OF_VIEW.on(
        (ctx) -> {
          if (!PerspectiveAPI.isEnabled()) return;
          ctx.fieldOfView = manager.modifyFov(ctx.fieldOfView);
        });

    // endregion

    // region lifecycle

    GameClientEvents.AFTER_MINECRAFT_INIT.on(
        minecraft -> {
          var stateManager = StateManagerImpl.getInstance(minecraft);
          if (Files.exists(stateManager.filePath())) {
            stateManager.tryLoadAndApply();
          }
        });
    GameClientEvents.ON_MINECRAFT_CLOSE.on(
        minecraft -> StateManagerImpl.getInstance(minecraft).tryExtractAndSave());

    // endregion

    // region internal events

    PerspectiveManager.INSTANCE.onCurrentPerspectiveChanged.on(
        perspective -> {
          if (!PerspectiveAPI.isEnabled()) return;
          LOGGER.debug("Switching current perspective to {}", perspective.id());
          Bridge.updateCameraType(perspective.cameraType());
        });

    // endregion
  }
}
