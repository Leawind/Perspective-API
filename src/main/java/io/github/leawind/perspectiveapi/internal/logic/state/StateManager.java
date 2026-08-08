package io.github.leawind.perspectiveapi.internal.logic.state;

import java.nio.file.Path;
import java.util.concurrent.Executor;
import org.jspecify.annotations.NonNull;

public interface StateManager {
  @NonNull Path filePath();

  void tryLoadAndApply();

  void tryExtractAndSave();

  void startAutoSave(@NonNull Executor clientExecutor);

  void stopAutoSave();
}
