package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import net.minecraft.client.CameraType;

public final class ModEvents {
  public static void register() {
    PerspectiveManagerImpl manager = PerspectiveManagerImpl.INSTANCE;

    GameClientEvents.CLIENT_TICK_START.on(
        minecraft -> {
          if (minecraft.level == null || minecraft.player == null) return;

          manager.resolveAndUpdatePerspective();
          var current = manager.getCurrentPerspective();
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

    GameClientEvents.AFTER_CLIENT_LEVEL_CHANGE.on(ignored -> manager.clearOverridesExceptCycler());

    // region camera

    GameClientEvents.SETUP_CAMERA.on((ctx) -> manager.updateCamera(ctx.partialTicks, ctx.camera));

    GameClientEvents.MODIFY_FIELD_OF_VIEW.on(
        (ctx) -> {
          Perspective perspective = manager.getCurrentPerspective();
          if (!perspective.shouldOverrideVanillaCamera()) return;
          ctx.fieldOfView = perspective.getFieldOfView(ctx.fieldOfView);
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
