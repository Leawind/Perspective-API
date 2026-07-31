package io.github.leawind.perspectiveapi.internal.logic.state;

import com.google.gson.JsonSyntaxException;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record StateManagerImpl(@NonNull Path filePath) implements StateManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(StateManagerImpl.class);
  private static volatile @Nullable StateManager instance = null;

  public static @NonNull StateManager getInstance(@NonNull Minecraft minecraft) {
    var manager = instance;
    if (manager == null) {
      String fileName = PerspectiveAPI.MOD_ID + ".json";
      Path filePath = minecraft.gameDirectory.toPath().resolve("config").resolve(fileName);
      instance = manager = new StateManagerImpl(filePath);
    }
    return manager;
  }

  @Override
  public void tryLoadAndApply() {
    try {
      LOGGER.info("Loading perspective state from {}", filePath);
      PerspectiveAPIState.load(filePath).apply();
    } catch (JsonSyntaxException e) {
      LOGGER.warn("Perspective state file is corrupted, skipping {}", filePath, e);
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Perspective state contains invalid values, skipping {}", filePath, e);
    } catch (IOException e) {
      LOGGER.warn("Failed to load perspective state from {}", filePath, e);
    }
  }

  @Override
  public void tryExtractAndSave() {
    try {
      Files.createDirectories(filePath.getParent());
      PerspectiveAPIState existingState = null;
      try {
        if (Files.exists(filePath)) {
          existingState = PerspectiveAPIState.load(filePath);
        }
      } catch (Exception e) {
        LOGGER.debug("Failed to read existing state from {}, proceeding to overwrite", filePath, e);
      }

      PerspectiveAPIState newState = PerspectiveAPIState.extract(existingState);
      if (newState.equals(existingState)) return;
      LOGGER.info("Saving perspective state to {}", filePath);
      newState.save(filePath);
    } catch (Exception e) {
      LOGGER.warn("Failed to save perspective state to {}.", filePath, e);
    }
  }
}
