package io.github.leawind.perspectiveapi.internal.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

public final class AtomicFileWriter {
  private AtomicFileWriter() {}

  public static void writeString(@NonNull Path path, @NonNull String content) throws IOException {
    Objects.requireNonNull(path);
    Objects.requireNonNull(content);

    Path target = path.toAbsolutePath();
    Path parent = target.getParent();
    if (parent == null) throw new IOException("Target path has no parent: " + path);
    Files.createDirectories(parent);

    Path temporary =
        Files.createTempFile(parent, "." + target.getFileName().toString() + ".", ".tmp");
    try {
      Files.writeString(temporary, content, StandardCharsets.UTF_8);
      try {
        Files.move(
            temporary,
            target,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
