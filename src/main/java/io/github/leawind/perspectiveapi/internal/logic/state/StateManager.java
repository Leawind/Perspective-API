package io.github.leawind.perspectiveapi.internal.logic.state;

import java.nio.file.Path;
import org.jspecify.annotations.NonNull;

public interface StateManager {
  @NonNull Path filePath();

  void tryLoadAndApply();

  void tryExtractAndSave();
}
