package io.github.leawind.perspectiveapi.internal.logic.state;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.jimfs.Jimfs;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateManagerImplTest {
  private FileSystem fs;
  private Path tempDir;

  @PerspectiveBehavior.Default
  @PerspectiveBehavior.Info(
      id = "perspective_api.first_person",
      baseType = BaseType.FIRST_PERSON,
      priority = 0)
  static class TestPerspective implements PerspectiveBehavior {
    static final TestPerspective INSTANCE = new TestPerspective();
  }

  @BeforeEach
  void beforeEach() {
    fs = Jimfs.newFileSystem();
    tempDir = fs.getPath("/tmp");
    PerspectiveRegistryImpl.INSTANCE.registerSilent(TestPerspective.INSTANCE);
  }

  @AfterEach
  void afterEach() throws IOException {
    PerspectiveAPI.setEnabled(true);
    PerspectiveAPI.setLogicTickInterval(PerspectiveAPI.DEFAULT_LOGIC_TICK_INTERVAL);
    PerspectiveAPI.getTransition().setDurationMs(260.0);
    PerspectiveAPI.getTransition().setBlendPower(0.6);
    fs.close();
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

  @Test
  void roundTripPreservesTransitionSettings() {
    Path filePath = tempDir.resolve("transition.json");
    StateManager manager = new StateManagerImpl(filePath);
    PerspectiveAPI.getTransition().setDurationMs(450.0);
    PerspectiveAPI.getTransition().setBlendPower(1.25);

    manager.tryExtractAndSave();
    PerspectiveAPI.getTransition().setDurationMs(1.0);
    PerspectiveAPI.getTransition().setBlendPower(1.0);
    manager.tryLoadAndApply();

    assertEquals(450.0, PerspectiveAPI.getTransition().getDurationMs());
    assertEquals(1.25, PerspectiveAPI.getTransition().getBlendPower());
  }

  @Test
  void roundTripPreservesLogicTickInterval() {
    Path filePath = tempDir.resolve("logic.json");
    StateManager manager = new StateManagerImpl(filePath);
    PerspectiveAPI.setLogicTickInterval(4);

    manager.tryExtractAndSave();
    PerspectiveAPI.setLogicTickInterval(1);
    manager.tryLoadAndApply();

    assertEquals(4, PerspectiveAPI.getLogicTickInterval());
  }

  @Test
  void invalidLogicTickIntervalIsNotPartiallyApplied() throws IOException {
    Path filePath = tempDir.resolve("invalid-logic.json");
    StateManager manager = new StateManagerImpl(filePath);
    Files.createDirectories(filePath.getParent());
    Files.writeString(
        filePath,
        """
        {
          "enabled": false,
          "logic_tick_interval": 0
        }
        """);
    PerspectiveAPI.setEnabled(true);

    manager.tryLoadAndApply();

    assertTrue(PerspectiveAPI.isEnabled());
    assertEquals(PerspectiveAPI.DEFAULT_LOGIC_TICK_INTERVAL, PerspectiveAPI.getLogicTickInterval());
  }

  @Test
  void invalidTransitionSettingsAreNotPartiallyApplied() throws IOException {
    Path filePath = tempDir.resolve("invalid-values.json");
    StateManager manager = new StateManagerImpl(filePath);
    Files.createDirectories(filePath.getParent());
    Files.writeString(
        filePath,
        """
        {
          "enabled": false,
          "transition.duration_ms": -1.0,
          "transition.blend_power": 1.0
        }
        """);
    PerspectiveAPI.setEnabled(true);

    manager.tryLoadAndApply();

    assertTrue(PerspectiveAPI.isEnabled());
  }
}
