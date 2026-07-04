package io.github.leawind.perspectiveapi.internal.impl.state;

import com.google.gson.JsonSyntaxException;
import io.github.leawind.perspectiveapi.api.state.StateManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record StateManagerImpl(@NonNull Path filePath) implements StateManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(StateManagerImpl.class);

  @Override
  public void tryLoadAndApply() {
    try {
      LOGGER.info("Loading perspective state from {}", filePath);
      PerspectiveApiState.load(filePath).apply();
    } catch (JsonSyntaxException e) {
      LOGGER.warn("Perspective state file is corrupted, skipping {}", filePath, e);
    } catch (IOException e) {
      LOGGER.warn("Failed to load perspective state from {}", filePath, e);
    }
  }

  @Override
  public void tryExtractAndSave() {
    try {
      Files.createDirectories(filePath.getParent());
      var newState = PerspectiveApiState.extract();
      try {
        if (Files.exists(filePath)) {
          var existingState = PerspectiveApiState.load(filePath);
          if (existingState.equals(newState)) {
            return;
          }
        }
      } catch (Exception e) {
        LOGGER.debug("Failed to read existing state from {}, proceeding to overwrite", filePath, e);
      }
      LOGGER.info("Saving perspective state to {}", filePath);
      newState.save(filePath);
    } catch (Exception e) {
      LOGGER.warn("Failed to save perspective state to {}.", filePath, e);
    }
  }
}
