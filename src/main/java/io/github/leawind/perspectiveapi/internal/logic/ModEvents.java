package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.mixin.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveTickContextImpl;
import io.github.leawind.perspectiveapi.utils.PerspectiveUtils;
import net.minecraft.client.CameraType;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModEvents {
  private static final Logger LOGGER = LoggerFactory.getLogger((ModEvents.class));

  public static void register() {

    GameClientEvents.HANDLE_KEYBINDS_START.on(
        (minecraft) -> {
          var options = minecraft.options;
          var manager = PerspectiveManager.get();

          while (options.keyTogglePerspective.consumeClick()) {
            manager.switchToNextAvailable();
          }
        });

    // region camera

    var context = new PerspectiveTickContextImpl();
    GameClientEvents.SETUP_CAMERA.on(
        (ctx) -> {
          Perspective perspective = PerspectiveManager.get().getActivePerspective();
          if (perspective instanceof VanillaPerspective) {
            return;
          }

          if (perspective == null) {
            return;
          }

          Entity entity = ((CameraAccessor) ctx.camera).getEntity();
          if (entity == null) {
            LOGGER.warn("Somehow camera entity is null");
            return;
          }
          context.setup(ctx.partialTicks, entity);
          perspective.tick(context);

          PerspectiveUtils.applyPerspectiveToCamera(perspective, ctx.camera);

          ctx.cancelDefault();
        });

    GameClientEvents.MODIFY_FIELD_OF_VIEW.on(
        (ctx) -> {
          Perspective perspective = PerspectiveManager.get().getActivePerspective();
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
