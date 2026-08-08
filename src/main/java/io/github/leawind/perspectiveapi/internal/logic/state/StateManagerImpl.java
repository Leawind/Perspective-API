package io.github.leawind.perspectiveapi.internal.logic.state;

import com.google.gson.JsonSyntaxException;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StateManagerImpl implements StateManager {
  static final long AUTO_SAVE_INTERVAL_SECONDS = 30;

  private static final Logger LOGGER = LoggerFactory.getLogger(StateManagerImpl.class);
  private static volatile @Nullable StateManager instance = null;

  private final Path filePath;
  private final AtomicBoolean autoSaveQueued = new AtomicBoolean();
  private @Nullable PerspectiveAPIState knownState;
  private @Nullable ScheduledExecutorService autoSaveExecutor;
  private boolean autoSaveStarted;
  private volatile boolean autoSaveRunning;

  public StateManagerImpl(@NonNull Path filePath) {
    this.filePath = Objects.requireNonNull(filePath);
  }

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
  public @NonNull Path filePath() {
    return filePath;
  }

  @Override
  public void tryLoadAndApply() {
    try {
      LOGGER.info("Loading perspective state from {}", filePath);
      PerspectiveAPIState loadedState = PerspectiveAPIState.load(filePath);
      loadedState.apply();
      knownState = loadedState;
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
      PerspectiveAPIState newState = PerspectiveAPIState.extract(knownState);
      if (newState.equals(knownState)) return;
      LOGGER.info("Saving perspective state to {}", filePath);
      newState.save(filePath);
      knownState = newState;
    } catch (Exception e) {
      LOGGER.warn("Failed to save perspective state to {}.", filePath, e);
    }
  }

  @Override
  public synchronized void startAutoSave(@NonNull Executor clientExecutor) {
    startAutoSave(clientExecutor, AUTO_SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  synchronized void startAutoSave(
      @NonNull Executor clientExecutor, long interval, @NonNull TimeUnit timeUnit) {
    Objects.requireNonNull(clientExecutor);
    Objects.requireNonNull(timeUnit);
    if (interval <= 0) throw new IllegalArgumentException("interval must be positive");
    if (autoSaveStarted) return;

    autoSaveStarted = true;
    autoSaveRunning = true;
    autoSaveExecutor =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "Perspective API State Saver");
              thread.setDaemon(true);
              return thread;
            });
    autoSaveExecutor.scheduleWithFixedDelay(
        () -> requestAutoSave(clientExecutor), interval, interval, timeUnit);
  }

  void requestAutoSave(@NonNull Executor clientExecutor) {
    Objects.requireNonNull(clientExecutor);
    if (!autoSaveRunning || !autoSaveQueued.compareAndSet(false, true)) return;
    try {
      clientExecutor.execute(this::runAutoSave);
    } catch (RuntimeException e) {
      autoSaveQueued.set(false);
      LOGGER.warn("Failed to schedule perspective state save on the client thread", e);
    }
  }

  private void runAutoSave() {
    try {
      if (autoSaveRunning) tryExtractAndSave();
    } finally {
      autoSaveQueued.set(false);
    }
  }

  @Override
  public void stopAutoSave() {
    ScheduledExecutorService executor;
    synchronized (this) {
      autoSaveRunning = false;
      executor = autoSaveExecutor;
      autoSaveExecutor = null;
    }
    if (executor != null) executor.shutdownNow();
  }
}
