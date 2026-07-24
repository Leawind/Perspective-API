package io.github.leawind.perspectiveapi.internal.utils;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/// Invokes third-party extension callbacks with consistent failure isolation.
public final class ExtensionInvoker {
  private final Logger logger;
  private final String extensionType;
  private final Sanitizer.ThrottledAction exceptionLog;

  public ExtensionInvoker(@NonNull Logger logger, @NonNull String extensionType) {
    this.logger = Objects.requireNonNull(logger);
    this.extensionType = Objects.requireNonNull(extensionType);
    exceptionLog = new Sanitizer.ThrottledAction(5000);
  }

  /// Runs a callback and reports whether it completed successfully.
  public boolean run(@NonNull String id, @NonNull String phase, @NonNull Runnable callback) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(phase);
    Objects.requireNonNull(callback);
    try {
      callback.run();
      return true;
    } catch (Throwable throwable) {
      report(id, phase, throwable);
      return false;
    }
  }

  /// Evaluates a boolean callback, returning `fallback` if it throws.
  public boolean testOrElse(
      @NonNull String id,
      @NonNull String phase,
      @NonNull BooleanSupplier callback,
      boolean fallback) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(phase);
    Objects.requireNonNull(callback);
    try {
      return callback.getAsBoolean();
    } catch (Throwable throwable) {
      report(id, phase, throwable);
      return fallback;
    }
  }

  /// Evaluates a callback, returning `fallback` if it throws.
  public <T> @Nullable T callOrElse(
      @NonNull String id,
      @NonNull String phase,
      @NonNull Supplier<? extends T> callback,
      @Nullable T fallback) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(phase);
    Objects.requireNonNull(callback);
    try {
      return callback.get();
    } catch (Throwable throwable) {
      report(id, phase, throwable);
      return fallback;
    }
  }

  /// Reports a callback failure according to the shared fatal-error and logging policy.
  public void report(@NonNull String id, @NonNull String phase, @NonNull Throwable throwable) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(phase);
    Objects.requireNonNull(throwable);
    Exceptions.rethrowIfFatal(throwable);
    exceptionLog.run(
        id + ":" + phase,
        () -> logger.warn("{} '{}' threw during {}", extensionType, id, phase, throwable));
  }
}
