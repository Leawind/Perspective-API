package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.logic.state.StateManagerImpl;
import java.nio.file.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Registers and handles mod event listeners.
@SuppressWarnings("ConstantConditions")
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

    GameClientEvents.AFTER_CLIENT_LEVEL_CHANGE.on(
        ignored -> {
          if (!PerspectiveAPI.isEnabled()) return;
          manager.overrides().clearExcept(PerspectiveSwitcherManager.KEY);
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
