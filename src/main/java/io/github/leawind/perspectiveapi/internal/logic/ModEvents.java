package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveCyclerImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Registers and handles mod event listeners.
public final class ModEvents {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModEvents.class);

  /// Registers all event handlers for client tick, keybinds, camera setup, and FOV modification.
  public static void register() {
    PerspectiveManagerImpl manager = PerspectiveManagerImpl.INSTANCE;

    GameClientEvents.CLIENT_TICK_START.on(
        minecraft -> {
          if (minecraft.level == null || minecraft.player == null) return;

          manager.resolveAndUpdateCurrentPerspective();
          var current = manager.getCurrent();
          current.clientTick(minecraft);
          if (!current.isAvailable()) {
            manager.cycler().switchToPreviousAvailable(manager.registry());
          }
        });

    GameClientEvents.HANDLE_KEYBINDS_START.on(
        (minecraft) -> {
          while (minecraft.options.keyTogglePerspective.consumeClick()) {
            manager.cycler().switchToNextAvailable(manager.registry());
          }
        });

    GameClientEvents.AFTER_CLIENT_LEVEL_CHANGE.on(
        ignored -> manager.overrides().clearExcept(PerspectiveCyclerImpl.KEY));

    // region camera

    GameClientEvents.SETUP_CAMERA.on((ctx) -> manager.updateCamera(ctx.partialTicks, ctx.camera));

    GameClientEvents.MODIFY_FIELD_OF_VIEW.on(
        (ctx) -> {
          Perspective perspective = manager.getCurrent();
          if (!perspective.shouldOverrideVanillaCamera()) return;
          ctx.fieldOfView = perspective.getFieldOfView(ctx.fieldOfView);
        });

    // endregion

    // region internal events

    PerspectiveManagerImpl.INSTANCE.onCurrentPerspectiveChanged.on(
        perspective -> {
          LOGGER.debug("Switching current perspective to {}", perspective.id());
          PerspectiveUtils.updateCameraType(perspective.cameraType());
        });

    // endregion
  }
}
