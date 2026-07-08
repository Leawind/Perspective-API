package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveWheelImpl;
import io.github.leawind.perspectiveapi.internal.logic.config.ConfigScreenManager;
import io.github.leawind.perspectiveapi.internal.logic.state.StateManagerImpl;
import io.github.leawind.perspectiveapi.internal.utils.KeyStateTracker;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Registers and handles mod event listeners.
public final class ModEvents {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModEvents.class);

  private static KeyStateTracker perspectiveKeyTracker;

  /// Registers all event handlers for client tick, keybinds, camera setup, and FOV modification.
  public static void register() {
    PerspectiveManager manager = PerspectiveManager.INSTANCE;

    GameClientEvents.CLIENT_TICK_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()) return;
          if (minecraft.level == null || minecraft.player == null) return;

          manager.clientTick(minecraft);
        });

    GameClientEvents.HANDLE_KEYBINDS_START.on(
        (minecraft) -> {
          if (!PerspectiveAPI.isEnabled()) return;

          KeyStateTracker.of(
                  minecraft.options.keyTogglePerspective,
                  builder ->
                      builder
                          .setHoldTicks(6)
                          .onPress(() -> PerspectiveManager.INSTANCE.wheel().cycleForward())
                          .onHold(() -> Bridge.setScreen(ConfigScreenManager.findAndBuild(null)))
                          .onHoldStop(() -> LOGGER.debug("Perspective key hold stop")))
              .tick()
              .drain();
        });

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
