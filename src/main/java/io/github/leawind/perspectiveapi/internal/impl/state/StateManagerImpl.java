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
      LOGGER.info("Saving perspective state to {}", filePath);
      Files.createDirectories(filePath.getParent());
      PerspectiveApiState.extract().save(filePath);
    } catch (Exception e) {
      LOGGER.warn("Failed to save perspective state to {}.", filePath, e);
    }
  }
}
