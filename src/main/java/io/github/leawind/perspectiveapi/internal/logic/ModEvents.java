package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;
import io.github.leawind.perspectiveapi.utils.PerspectiveUtils;
import net.minecraft.client.CameraType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModEvents {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModEvents.class);

  public static void register() {
    PerspectiveManagerImpl manager = PerspectiveManagerImpl.INSTANCE;

    GameClientEvents.CLIENT_TICK_START.on(
        minecraft -> {
          if (minecraft.level != null && minecraft.player != null) {
            var active = manager.getActivePerspective();
            if (active != null) {
              active.clientTick(minecraft);

              if (!active.isAvailable()) {
                manager.switchToPreviousAvailable();
              }
            }
          }
        });

    GameClientEvents.HANDLE_KEYBINDS_START.on(
        (minecraft) -> {
          var options = minecraft.options;

          while (options.keyTogglePerspective.consumeClick()) {
            manager.switchToNextAvailable();
          }
        });

    // region camera

    GameClientEvents.SETUP_CAMERA.on(
        (ctx) -> {
          manager.updateCamera(ctx.partialTicks, ctx.camera);
          ctx.cancelDefault();
        });

    GameClientEvents.MODIFY_FIELD_OF_VIEW.on(
        (ctx) -> {
          Perspective perspective = manager.getActivePerspective();
          if (perspective != null) {
            ctx.fieldOfView = perspective.getFieldOfView(ctx.fieldOfView);
          }
        });

    // endregion

    // region internal events

    PerspectiveManagerImpl.INSTANCE.onActivePerspectiveChanged.on(
        perspective ->
            PerspectiveUtils.updateCameraType(
                perspective == null ? CameraType.FIRST_PERSON : perspective.cameraType()));

    // endregion
  }
}
