package io.github.leawind.perspectiveapi.internal.logic.state;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.jimfs.Jimfs;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.logic.ModEntrypoint;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateManagerImplTest {
  private FileSystem fs;
  private Path tempDir;

  @BeforeEach
  void beforeEach() {
    fs = Jimfs.newFileSystem();
    tempDir = fs.getPath("/tmp");
    ModEntrypoint.initialize();
  }

  @Test
  void filePathReturnsCorrectPath() {
    Path filePath = tempDir.resolve("test.json");
    StateManager manager = new StateManagerImpl(filePath);
    assertEquals(filePath, manager.filePath());
  }

  @Test
  void tryExtractAndSaveCreatesParentDirectories() throws IOException {
    Path filePath = tempDir.resolve("subdir").resolve("nested").resolve("test.json");
    StateManager manager = new StateManagerImpl(filePath);

    assertFalse(Files.exists(filePath));
    manager.tryExtractAndSave();
    assertTrue(Files.exists(filePath));
  }

  @Test
  void tryLoadAndApplyWithNonExistentFileDoesNotThrow() {
    Path filePath = tempDir.resolve("nonexistent.json");
    StateManager manager = new StateManagerImpl(filePath);

    assertDoesNotThrow(manager::tryLoadAndApply);
  }

  @Test
  void tryLoadAndApplyWithCorruptedFileKeepsFile() throws IOException {
    Path filePath = tempDir.resolve("corrupted.json");
    StateManager manager = new StateManagerImpl(filePath);

    Files.createDirectories(filePath.getParent());
    Files.writeString(filePath, "{invalid json!!!");
    assertTrue(Files.exists(filePath));

    manager.tryLoadAndApply();
    assertTrue(Files.exists(filePath));
  }

  @Test
  void roundTripPreservesEnabledState() {
    Path filePath = tempDir.resolve("roundtrip.json");
    StateManager manager = new StateManagerImpl(filePath);

    PerspectiveAPI.setEnabled(false);
    manager.tryExtractAndSave();

    PerspectiveAPI.setEnabled(true);
    manager.tryLoadAndApply();
    assertFalse(PerspectiveAPI.isEnabled());

    PerspectiveAPI.setEnabled(true);
    manager.tryExtractAndSave();

    PerspectiveAPI.setEnabled(false);
    manager.tryLoadAndApply();
    assertTrue(PerspectiveAPI.isEnabled());
  }
}
