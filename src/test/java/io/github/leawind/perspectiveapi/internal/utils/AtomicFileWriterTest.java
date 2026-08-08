package io.github.leawind.perspectiveapi.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AtomicFileWriterTest {

  @Test
  void writeStringCreatesParentsAndReplacesExistingContent() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem()) {
      Path path = fs.getPath("/config/nested/state.json");

      AtomicFileWriter.writeString(path, "first");
      AtomicFileWriter.writeString(path, "second");

      assertEquals("second", Files.readString(path));
      try (var files = Files.list(path.getParent())) {
        assertEquals(1, files.count());
      }
    }
  }

  @Test
  void writeStringCleansUpTemporaryFileAfterFailure() throws IOException {
    try (FileSystem fs = Jimfs.newFileSystem()) {
      Path parent = fs.getPath("/config");
      Path target = parent.resolve("state.json");
      Files.createDirectories(target.resolve("child"));

      assertThrows(IOException.class, () -> AtomicFileWriter.writeString(target, "state"));

      try (var files = Files.list(parent)) {
        assertEquals(1, files.count());
      }
    }
  }
}
