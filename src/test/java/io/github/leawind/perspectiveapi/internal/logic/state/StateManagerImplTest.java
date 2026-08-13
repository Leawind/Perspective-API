package io.github.leawind.perspectiveapi.internal.logic.state;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.jimfs.Jimfs;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.impl.TransitionImpl;
import io.github.leawind.perspectiveapi.internal.impl.transition.position.FixedStartPositionTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.rotation.ChasingRotationTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.FixedStartFovTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.FixedStartOrthographicHeightTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import io.github.leawind.perspectiveapi.internal.logic.builtin.selection.PerspectiveSwitcher;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateManagerImplTest {
  private static final TestStateSection TEST_STATE_SECTION = new TestStateSection();

  static {
    PerspectiveAPIState.registerSection(TEST_STATE_SECTION);
  }

  private static final class TestStateSection implements PerspectiveAPIState.Section<String> {
    private static final String ID = "test.state_section";
    private String value = "default";

    @Override
    public @NonNull String stateId() {
      return ID;
    }

    @Override
    public @NonNull Codec<String> stateCodec() {
      return Codec.STRING;
    }

    @Override
    public @NonNull String extractState() {
      return value;
    }

    @Override
    public void applyState(@NonNull String state) {
      value = state;
    }
  }

  private FileSystem fs;
  private Path tempDir;

  @PerspectiveInfo.Default
  @PerspectiveInfo.Declaration(
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
    if (!PerspectiveRegistryImpl.INSTANCE.contains("perspective_api.first_person")) {
      PerspectiveRegistryImpl.INSTANCE.registerSilent(TestPerspective.INSTANCE);
    }
  }

  @AfterEach
  void afterEach() throws IOException {
    PerspectiveAPI.setEnabled(true);
    PerspectiveAPI.getTransition().setDurationMs(260.0);
    PerspectiveManager.INSTANCE
        .transition()
        .setPositionAlgorithm(TransitionImpl.DEFAULT_POSITION_ALGORITHM);
    PerspectiveManager.INSTANCE
        .transition()
        .setRotationAlgorithm(TransitionImpl.DEFAULT_ROTATION_ALGORITHM);
    PerspectiveManager.INSTANCE.transition().setFovAlgorithm(TransitionImpl.DEFAULT_FOV_ALGORITHM);
    PerspectiveManager.INSTANCE
        .transition()
        .setOrthographicHeightAlgorithm(TransitionImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT_ALGORITHM);
    PerspectiveManager.INSTANCE.restoreSelection(null);
    TEST_STATE_SECTION.value = "default";
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
  void roundTripPreservesTransitionDuration() {
    Path filePath = tempDir.resolve("transition.json");
    StateManager manager = new StateManagerImpl(filePath);
    PerspectiveAPI.getTransition().setDurationMs(450.0);

    manager.tryExtractAndSave();
    PerspectiveAPI.getTransition().setDurationMs(1.0);
    manager.tryLoadAndApply();

    assertEquals(450.0, PerspectiveAPI.getTransition().getDurationMs());
  }

  @Test
  void roundTripDoesNotPersistDebugTransitionAlgorithms() throws IOException {
    Path filePath = tempDir.resolve("transition-algorithm-types.json");
    StateManager manager = new StateManagerImpl(filePath);
    PerspectiveManager.INSTANCE
        .transition()
        .setPositionAlgorithm(FixedStartPositionTransitionAlgorithm.INSTANCE);
    PerspectiveManager.INSTANCE
        .transition()
        .setRotationAlgorithm(ChasingRotationTransitionAlgorithm.INSTANCE);
    PerspectiveManager.INSTANCE
        .transition()
        .setFovAlgorithm(FixedStartFovTransitionAlgorithm.INSTANCE);
    PerspectiveManager.INSTANCE
        .transition()
        .setOrthographicHeightAlgorithm(FixedStartOrthographicHeightTransitionAlgorithm.INSTANCE);

    manager.tryExtractAndSave();
    var savedState = JsonParser.parseString(Files.readString(filePath)).getAsJsonObject();
    PerspectiveManager.INSTANCE
        .transition()
        .setPositionAlgorithm(TransitionImpl.DEFAULT_POSITION_ALGORITHM);
    PerspectiveManager.INSTANCE
        .transition()
        .setRotationAlgorithm(TransitionImpl.DEFAULT_ROTATION_ALGORITHM);
    PerspectiveManager.INSTANCE.transition().setFovAlgorithm(TransitionImpl.DEFAULT_FOV_ALGORITHM);
    PerspectiveManager.INSTANCE
        .transition()
        .setOrthographicHeightAlgorithm(TransitionImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT_ALGORITHM);
    manager.tryLoadAndApply();

    assertFalse(savedState.has("transition.position_algorithm"));
    assertFalse(savedState.has("transition.rotation_algorithm"));
    assertFalse(savedState.has("transition.fov_algorithm"));
    assertFalse(savedState.has("transition.orthographic_height_algorithm"));
    assertFalse(savedState.has("transition.fixed_start_chasing_rotation.blend_power"));
    assertEquals(
        TransitionImpl.DEFAULT_POSITION_ALGORITHM,
        PerspectiveManager.INSTANCE.transition().getPositionAlgorithm());
    assertEquals(
        TransitionImpl.DEFAULT_ROTATION_ALGORITHM,
        PerspectiveManager.INSTANCE.transition().getRotationAlgorithm());
    assertEquals(
        TransitionImpl.DEFAULT_FOV_ALGORITHM,
        PerspectiveManager.INSTANCE.transition().getFovAlgorithm());
    assertEquals(
        TransitionImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT_ALGORITHM,
        PerspectiveManager.INSTANCE.transition().getOrthographicHeightAlgorithm());
  }

  @Test
  void roundTripPersistsSelectionInsteadOfResolvedCurrentPerspective() throws IOException {
    Path filePath = tempDir.resolve("selection.json");
    StateManager manager = new StateManagerImpl(filePath);
    PerspectiveManager.INSTANCE.restoreSelection("test.raw_selection");

    manager.tryExtractAndSave();
    var savedState = JsonParser.parseString(Files.readString(filePath)).getAsJsonObject();
    PerspectiveManager.INSTANCE.restoreSelection("test.changed");
    manager.tryLoadAndApply();

    assertEquals(
        "test.raw_selection", PerspectiveManager.INSTANCE.getSelected());
    assertEquals("test.raw_selection", savedState.get("selection.current").getAsString());
    assertFalse(savedState.has("manager.current"));
  }

  @Test
  void roundTripPreservesRegisteredStateSection() {
    Path filePath = tempDir.resolve("section.json");
    StateManager manager = new StateManagerImpl(filePath);
    TEST_STATE_SECTION.value = "saved";

    manager.tryExtractAndSave();
    TEST_STATE_SECTION.value = "changed";
    manager.tryLoadAndApply();

    assertEquals("saved", TEST_STATE_SECTION.value);
  }

  @Test
  void saveIncludesPerspectiveSwitcherStateByStableId() throws IOException {
    Path filePath = tempDir.resolve("orbit-section.json");
    StateManager manager = new StateManagerImpl(filePath);

    manager.tryExtractAndSave();

    var root = JsonParser.parseString(Files.readString(filePath)).getAsJsonObject();
    assertTrue(
        root.getAsJsonObject("sections").has(PerspectiveSwitcher.ID),
        "Perspective switcher should register its state section during initialization");
  }

  @Test
  void invalidStateSectionDoesNotPreventCoreStateFromLoading() throws IOException {
    Path filePath = tempDir.resolve("invalid-section.json");
    StateManager manager = new StateManagerImpl(filePath);
    Files.createDirectories(filePath.getParent());
    Files.writeString(
        filePath,
        """
        {
          "enabled": false,
          "sections": {
            "test.state_section": 42
          }
        }
        """);
    PerspectiveAPI.setEnabled(true);
    TEST_STATE_SECTION.value = "unchanged";

    manager.tryLoadAndApply();

    assertFalse(PerspectiveAPI.isEnabled());
    assertEquals("unchanged", TEST_STATE_SECTION.value);
  }

  @Test
  void savePreservesLoadedUnknownSectionsAndIgnoresExternalChanges() throws IOException {
    Path filePath = tempDir.resolve("unknown-section.json");
    StateManager manager = new StateManagerImpl(filePath);
    Files.createDirectories(filePath.getParent());
    Files.writeString(
        filePath,
        """
        {
          "sections": {
            "unknown.section": {
              "answer": 42
            }
          }
        }
        """);
    manager.tryLoadAndApply();
    manager.tryExtractAndSave();

    Files.writeString(
        filePath,
        """
        {
          "sections": {
            "unknown.section": {
              "answer": 99
            },
            "external.section": true
          }
        }
        """);

    manager.tryExtractAndSave();
    assertTrue(
        JsonParser.parseString(Files.readString(filePath))
            .getAsJsonObject()
            .getAsJsonObject("sections")
            .has("external.section"));

    PerspectiveAPI.setEnabled(false);
    manager.tryExtractAndSave();

    var root = JsonParser.parseString(Files.readString(filePath)).getAsJsonObject();
    assertEquals(
        42,
        root.getAsJsonObject("sections")
            .getAsJsonObject("unknown.section")
            .get("answer")
            .getAsInt());
    assertFalse(root.getAsJsonObject("sections").has("external.section"));
  }

  @Test
  void saveWithoutLoadingOverwritesExistingContent() throws IOException {
    Path filePath = tempDir.resolve("overwrite.json");
    StateManager manager = new StateManagerImpl(filePath);
    Files.createDirectories(filePath.getParent());
    Files.writeString(
        filePath,
        """
        {
          "external": true
        }
        """);

    manager.tryExtractAndSave();

    var root = JsonParser.parseString(Files.readString(filePath)).getAsJsonObject();
    assertFalse(root.has("external"));
  }

  @Test
  void autoSaveRequestsAreCoalescedUntilTheClientTaskRuns() {
    Path filePath = tempDir.resolve("coalesced.json");
    StateManagerImpl manager = new StateManagerImpl(filePath);
    var clientTasks = new LinkedBlockingQueue<Runnable>();
    manager.startAutoSave(clientTasks::add, 1, TimeUnit.DAYS);
    try {
      manager.requestAutoSave(clientTasks::add);
      manager.requestAutoSave(clientTasks::add);

      assertEquals(1, clientTasks.size());
      Runnable task = clientTasks.poll();
      assertNotNull(task);
      task.run();

      manager.requestAutoSave(clientTasks::add);
      assertEquals(1, clientTasks.size());
    } finally {
      manager.stopAutoSave();
    }
  }

  @Test
  void autoSaveSchedulerUsesElapsedTime() throws InterruptedException {
    StateManagerImpl manager = new StateManagerImpl(tempDir.resolve("scheduled.json"));
    var clientTasks = new LinkedBlockingQueue<Runnable>();
    manager.startAutoSave(clientTasks::add, 10, TimeUnit.MILLISECONDS);
    try {
      assertNotNull(clientTasks.poll(2, TimeUnit.SECONDS));
      assertNull(clientTasks.poll(50, TimeUnit.MILLISECONDS));
    } finally {
      manager.stopAutoSave();
    }
  }

  @Test
  void stoppingAutoSaveDiscardsQueuedTask() {
    Path filePath = tempDir.resolve("stopped.json");
    StateManagerImpl manager = new StateManagerImpl(filePath);
    var clientTasks = new LinkedBlockingQueue<Runnable>();
    manager.startAutoSave(clientTasks::add, 1, TimeUnit.DAYS);
    manager.requestAutoSave(clientTasks::add);
    Runnable task = clientTasks.poll();
    assertNotNull(task);

    manager.stopAutoSave();
    task.run();

    assertFalse(Files.exists(filePath));
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
          "transition.duration_ms": -1.0
        }
        """);
    PerspectiveAPI.setEnabled(true);

    manager.tryLoadAndApply();

    assertTrue(PerspectiveAPI.isEnabled());
  }
}
