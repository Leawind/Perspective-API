package io.github.leawind.perspectiveapi.internal.utils;

public final class Exceptions {
  private Exceptions() {}

  /// Rethrows errors from which the framework cannot safely recover.
  @SuppressWarnings("removal")
  public static void rethrowIfFatal(Throwable throwable) {
    if (throwable instanceof VirtualMachineError error) throw error;
    if (throwable instanceof ThreadDeath error) throw error;
  }

  /// Converts a non-fatal throwable to an unchecked exception.
  public static RuntimeException propagate(Throwable throwable) {
    rethrowIfFatal(throwable);
    if (throwable instanceof RuntimeException exception) return exception;
    if (throwable instanceof Error error) throw error;
    return new RuntimeException(throwable);
  }
}
