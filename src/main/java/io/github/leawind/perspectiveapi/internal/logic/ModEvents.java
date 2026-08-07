package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.logic.state.StateManagerImpl;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("ConstantConditions")
public final class ModEvents {
  private static final Logger LOGGER = LoggerFactory.getLogger(ModEvents.class);

  public static void register() {
    PerspectiveManager manager = PerspectiveManager.INSTANCE;

    GameClientEvents.BEFORE_MAIN_CAMERA_UPDATE.on(
        () -> {
          Minecraft minecraft = Minecraft.getInstance();
          if (!PerspectiveAPI.isEnabled() || minecraft.level == null || minecraft.player == null)
            return;
          manager.beforeMainCameraUpdate();
        });

    GameClientEvents.SETUP_CAMERA.on(
        (ctx) -> {
          if (!PerspectiveAPI.isEnabled()) return;
          manager.updateCamera(ctx.partialTicks, ctx.camera);
        });

    GameClientEvents.MODIFY_FIELD_OF_VIEW.on(
        (ctx) -> {
          if (!PerspectiveAPI.isEnabled()) return;
          ctx.fieldOfViewDeg = manager.modifyFov(ctx.fieldOfViewDeg);
        });

    GameClientEvents.MODIFY_PROJECTION.on(
        (ctx) -> {
          if (!PerspectiveAPI.isEnabled()) return;
          manager.modifyProjection(ctx);
        });

    GameClientEvents.AFTER_MINECRAFT_INIT.on(
        minecraft -> {
          var stateManager = StateManagerImpl.getInstance(minecraft);
          if (Files.exists(stateManager.filePath())) {
            stateManager.tryLoadAndApply();
          }
        });
    GameClientEvents.ON_MINECRAFT_CLOSE.on(
        minecraft -> StateManagerImpl.getInstance(minecraft).tryExtractAndSave());
  }
}
