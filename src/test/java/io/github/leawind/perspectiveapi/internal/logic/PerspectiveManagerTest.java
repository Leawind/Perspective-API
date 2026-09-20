package io.github.leawind.perspectiveapi.internal.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import io.github.leawind.perspectiveapi.api.PerspectiveModifier;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierContext;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.ProjectionMode;
import io.github.leawind.perspectiveapi.internal.bridge.CameraOperations;
import io.github.leawind.perspectiveapi.internal.bridge.CameraSpace;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyProjectionContext;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerspectiveManagerTest {

  private static final Vector3dc VANILLA_POSITION = new Vector3d(1, 2, 3);
  private static final Vector3dc TARGET_POSITION = new Vector3d(10, 20, 30);

  private PerspectiveRegistryImpl registry;
  private RecordingCameraOperations cameraOps;
  private FakeClock clock;
  private boolean apiEnabled;
  private PerspectiveManager manager;
  private FakeBehavior defaultBehavior;

  @BeforeEach
  void beforeEach() {
    registry = new PerspectiveRegistryImpl();
    cameraOps = new RecordingCameraOperations();
    clock = new FakeClock();
    apiEnabled = true;
    manager = new PerspectiveManager(registry, null, cameraOps, clock, () -> apiEnabled);
    defaultBehavior = new FakeBehavior("test.default");
    registerAsDefault(defaultBehavior);
  }

  /// Resolves the current perspective and runs one camera state pipeline pass.
  private void runFrame() {
    manager.beforeMainCameraUpdate();
    manager.runCameraPipeline(0.5f, cameraOps.mainCamera, null);
  }

  private Perspective registerBehavior(FakeBehavior behavior) {
    PerspectiveInfo info =
        PerspectiveInfo.builder(behavior.id, Component.literal(behavior.id)).build();
    registry.register(info, behavior);
    return registry.get(behavior.id);
  }

  private void registerAsDefault(FakeBehavior behavior) {
    PerspectiveInfo info =
        PerspectiveInfo.builder(behavior.id, Component.literal(behavior.id)).build();
    registry.registerDefault(info, 0, behavior);
  }

  private Vector3dc lastWrittenPosition() {
    return cameraOps.writtenPositions.get(cameraOps.writtenPositions.size() - 1);
  }

  /// The rotation written back to the camera when no one modifies the vanilla rotation.
  private static Quaternionfc roundTripRotation() {
    Quaternionf api = new Quaternionf();
    CameraSpace.mcToApi(new Quaternionf(), api);
    return CameraSpace.apiToMc(api, new Quaternionf());
  }

  // region construction

  @Test
  void constructorRejectsNullCollaborators() {
    assertThrows(
        NullPointerException.class,
        () -> new PerspectiveManager(null, null, cameraOps, clock, () -> apiEnabled));
    assertThrows(
        NullPointerException.class,
        () -> new PerspectiveManager(registry, null, null, clock, () -> apiEnabled));
    assertThrows(
        NullPointerException.class,
        () -> new PerspectiveManager(registry, null, cameraOps, null, () -> apiEnabled));
    assertThrows(
        NullPointerException.class,
        () -> new PerspectiveManager(registry, null, cameraOps, clock, null));
  }

  // endregion

  // region resolution

  @Test
  void resolvesDefaultWhenSelectionIsEmpty() {
    runFrame();

    assertEquals("test.default", manager.getCurrent().info().id());
    assertEquals(List.of(CameraType.FIRST_PERSON), cameraOps.cameraTypeChanges);
  }

  @Test
  void selectionChangeActivatesNewPerspectiveOnNextFrame() {
    FakeBehavior target = new FakeBehavior("test.target");
    target.baseType = BaseType.THIRD_PERSON_BACK;
    registerBehavior(target);
    runFrame();

    manager.restoreSelection("test.target");
    assertEquals("test.default", manager.getCurrent().info().id());

    runFrame();

    assertEquals("test.target", manager.getCurrent().info().id());
    assertEquals(List.of("test.default:activate", "test.default:deactivate"), defaultBehavior.events);
    assertEquals(List.of("test.target:activate"), target.events);
    assertEquals(
        List.of(CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_BACK),
        cameraOps.cameraTypeChanges);
  }

  @Test
  void overridesTakePrecedenceOverSelectionAndSelectionSurvivesBeneath() {
    FakeBehavior target = new FakeBehavior("test.target");
    FakeBehavior alt = new FakeBehavior("test.alt");
    registerBehavior(target);
    registerBehavior(alt);

    manager.restoreSelection("test.target");
    runFrame();
    assertEquals("test.target", manager.getCurrent().info().id());

    var registration = manager.overrides().register("test.override", 10, () -> "test.alt");
    runFrame();
    assertEquals("test.alt", manager.getCurrent().info().id());

    assertTrue(registration.unregister());
    runFrame();
    assertEquals("test.target", manager.getCurrent().info().id());
  }

  @Test
  void resolutionSkipsUnavailableIdsAndFallsBackToDefault() {
    FakeBehavior target = new FakeBehavior("test.target");
    registerBehavior(target);

    target.available = false;
    manager.restoreSelection("test.target");
    runFrame();
    assertEquals("test.default", manager.getCurrent().info().id());

    manager.overrides().register("test.override", 0, () -> "test.target");
    runFrame();
    assertEquals("test.default", manager.getCurrent().info().id());
  }

  // endregion

  // region lifecycle and listeners

  @Test
  void switchRunsDeactivationActivationThenListenersInOrder() {
    List<String> log = new ArrayList<>();
    FakeBehavior target = new FakeBehavior("test.target", log);
    registerBehavior(target);
    runFrame();
    defaultBehavior.logTo(log);
    log.clear();
    manager.onCurrentChanged((from, to) -> log.add("listener:" + to.info().id()));

    manager.restoreSelection("test.target");
    runFrame();

    assertEquals(
        List.of("test.default:deactivate", "test.target:activate", "listener:test.target"), log);
  }

  @Test
  void currentPerspectiveIsAlreadyNewInsideChangeListener() {
    FakeBehavior target = new FakeBehavior("test.target");
    registerBehavior(target);
    runFrame();
    AtomicReference<Perspective> seen = new AtomicReference<>();

    manager.onCurrentChanged((from, to) -> seen.set(manager.getCurrent()));
    manager.restoreSelection("test.target");
    runFrame();

    assertEquals("test.target", seen.get().info().id());
  }

  @Test
  void changesTriggeredInsideListenerAreQueuedUntilNotificationEnds() {
    List<String> log = new ArrayList<>();
    FakeBehavior target = new FakeBehavior("test.target", log);
    FakeBehavior alt = new FakeBehavior("test.alt", log);
    registerBehavior(target);
    registerBehavior(alt);

    AtomicBoolean reentered = new AtomicBoolean();
    manager.restoreSelection("test.target");
    manager.onCurrentChanged(
        (from, to) -> {
          log.add("listener:" + to.info().id());
          if (to.info().id().equals("test.target") && reentered.compareAndSet(false, true)) {
            manager.restoreSelection("test.alt");
            manager.beforeMainCameraUpdate();
          }
        });

    runFrame();

    assertEquals(
        List.of(
            "test.target:activate",
            "listener:test.target",
            "test.target:deactivate",
            "test.alt:activate",
            "listener:test.alt"),
        log);
  }

  @Test
  void failingListenerDoesNotPreventOtherListeners() {
    FakeBehavior target = new FakeBehavior("test.target");
    registerBehavior(target);
    runFrame();
    List<String> received = new ArrayList<>();

    manager.onCurrentChanged(
        (from, to) -> {
          throw new IllegalStateException("listener failure");
        });
    manager.onCurrentChanged((from, to) -> received.add(to.info().id()));

    manager.restoreSelection("test.target");
    runFrame();

    assertEquals(List.of("test.target"), received);
  }

  @Test
  void failingActivationDoesNotAbortSwitch() {
    FakeBehavior target = new FakeBehavior("test.target");
    target.throwOnActivate = true;
    registerBehavior(target);
    runFrame();

    manager.restoreSelection("test.target");
    runFrame();

    assertEquals("test.target", manager.getCurrent().info().id());
    assertEquals(2, cameraOps.writtenPositions.size());
  }

  @Test
  void failingBaseTypeReportKeepsCurrentVanillaCameraType() {
    defaultBehavior.throwOnBaseType = true;

    runFrame();

    assertTrue(cameraOps.cameraTypeChanges.isEmpty());
  }

  @Test
  void writesVanillaCameraTypeOnlyWhenChanged() {
    FakeBehavior target = new FakeBehavior("test.target");
    target.baseType = BaseType.THIRD_PERSON_FRONT;
    registerBehavior(target);

    runFrame();
    runFrame();
    assertEquals(List.of(CameraType.FIRST_PERSON), cameraOps.cameraTypeChanges);

    manager.restoreSelection("test.target");
    runFrame();
    runFrame();
    assertEquals(
        List.of(CameraType.FIRST_PERSON, CameraType.THIRD_PERSON_FRONT),
        cameraOps.cameraTypeChanges);
  }

  // endregion

  // region pipeline

  @Test
  void pipelineAppliesPerspectiveAndModifiersAndPublishesSnapshot() {
    List<String> order = new ArrayList<>();
    FakeBehavior target = new FakeBehavior("test.target", order);
    target.position = TARGET_POSITION;
    target.fovDeg = 100f;
    registerBehavior(target);
    manager
        .modifiers()
        .register(
            "test.mod",
            0,
            new PerspectiveModifier() {
              @Override
              public void apply(
                  PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveModifierContext ctx) {
                order.add("modifier");
                assertEquals(100f, ctx.perspectiveBaseState().getFovDeg());
                state.position().add(1, 0, 0);
              }
            });

    manager.restoreSelection("test.target");
    runFrame();

    assertEquals(List.of("test.target:activate", "modifier"), order);
    assertEquals(1, cameraOps.writtenPositions.size());
    TestUtils.assertVectorEquals(new Vector3d(11, 20, 30), lastWrittenPosition());
    TestUtils.assertQuatEquals(roundTripRotation(), cameraOps.writtenRotations.get(0));

    assertEquals(1, target.resolvedStates.size());
    TestUtils.assertVectorEquals(new Vector3d(11, 20, 30), target.resolvedStates.get(0).position());
    assertEquals(100f, target.resolvedStates.get(0).fovDeg());

    PerspectiveState snapshot = manager.getPreviousCameraState();
    assertNotNull(snapshot);
    TestUtils.assertVectorEquals(new Vector3d(11, 20, 30), snapshot.position());
    assertEquals(100f, snapshot.getFovDeg());
  }

  @Test
  void pipelineFallsBackToVanillaStateWhenPerspectiveThrows() {
    FakeBehavior target = new FakeBehavior("test.target");
    target.throwOnCompute = true;
    registerBehavior(target);
    List<String> order = new ArrayList<>();
    manager
        .modifiers()
        .register(
            "test.mod",
            0,
            new PerspectiveModifier() {
              @Override
              public void apply(
                  PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveModifierContext ctx) {
                order.add("modifier");
              }
            });

    manager.restoreSelection("test.target");
    runFrame();

    assertEquals(List.of("modifier"), order);
    assertEquals(1, cameraOps.writtenPositions.size());
    TestUtils.assertVectorEquals(VANILLA_POSITION, lastWrittenPosition());

    assertEquals(1, target.resolvedStates.size());
    assertEquals(70f, target.resolvedStates.get(0).fovDeg());
    TestUtils.assertVectorEquals(VANILLA_POSITION, manager.getPreviousCameraState().position());
  }

  @Test
  void pipelineRollsBackOnlyInvalidFields() {
    FakeBehavior target = new FakeBehavior("test.target");
    target.position = TARGET_POSITION;
    target.setNaNFov = true;
    registerBehavior(target);

    manager.restoreSelection("test.target");
    runFrame();

    TestUtils.assertVectorEquals(TARGET_POSITION, lastWrittenPosition());
    assertEquals(70f, target.resolvedStates.get(0).fovDeg());
    assertEquals(70f, manager.modifyFov(50f));
  }

  @Test
  void pipelineContinuesWhenModifierThrows() {
    FakeBehavior target = new FakeBehavior("test.target");
    target.position = TARGET_POSITION;
    registerBehavior(target);
    manager
        .modifiers()
        .register(
            "test.mod",
            0,
            new PerspectiveModifier() {
              @Override
              public void apply(
                  PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveModifierContext ctx) {
                state.position().add(5, 0, 0);
                throw new IllegalStateException("modifier failure");
              }
            });

    manager.restoreSelection("test.target");
    runFrame();

    assertEquals(1, cameraOps.writtenPositions.size());
    TestUtils.assertVectorEquals(TARGET_POSITION, lastWrittenPosition());
    TestUtils.assertVectorEquals(TARGET_POSITION, manager.getPreviousCameraState().position());
    assertEquals(1, target.resolvedStates.size());
  }

  @Test
  void disabledApiRunsPipelineWithoutPublishingSnapshot() {
    FakeBehavior target = new FakeBehavior("test.target");
    registerBehavior(target);

    apiEnabled = false;
    manager.restoreSelection("test.target");
    runFrame();

    assertEquals("test.target", manager.getCurrent().info().id());
    assertEquals(1, cameraOps.writtenPositions.size());
    assertNull(manager.getPreviousCameraState());
  }

  // endregion

  // region transition

  @Test
  void transitionsDisabledByDefaultApplyTargetStateImmediately() {
    FakeBehavior target = new FakeBehavior("test.target");
    target.position = TARGET_POSITION;
    registerBehavior(target);

    runFrame();

    manager.restoreSelection("test.target");
    runFrame();
    TestUtils.assertVectorEquals(TARGET_POSITION, lastWrittenPosition());

    clock.advance(130);
    runFrame();
    TestUtils.assertVectorEquals(TARGET_POSITION, lastWrittenPosition());
  }

  @Test
  void disablingTransitionsMidFlightAppliesTargetStateImmediately() {
    manager.transition().setEnabled(true);
    FakeBehavior target = new FakeBehavior("test.target");
    target.position = TARGET_POSITION;
    registerBehavior(target);

    runFrame();

    manager.restoreSelection("test.target");
    runFrame();
    assertTrue(lastWrittenPosition().distance(new Vector3d(TARGET_POSITION)) > 1.0);

    manager.transition().setEnabled(false);
    runFrame();
    TestUtils.assertVectorEquals(TARGET_POSITION, lastWrittenPosition());
  }

  @Test
  void transitionInterpolatesWithinWindowAndSettlesAfterIt() {
    manager.transition().setEnabled(true);
    FakeBehavior target = new FakeBehavior("test.target");
    target.position = TARGET_POSITION;
    registerBehavior(target);

    runFrame();
    TestUtils.assertVectorEquals(VANILLA_POSITION, lastWrittenPosition());

    manager.restoreSelection("test.target");
    runFrame();
    Vector3dc immediate = lastWrittenPosition();
    assertTrue(immediate.distance(new Vector3d(TARGET_POSITION)) > 1.0, "not yet at target");

    clock.advance(130);
    runFrame();
    Vector3dc mid = lastWrittenPosition();
    double[] start = {1, 2, 3};
    double[] targetComponents = {10, 20, 30};
    double[] midComponents = {mid.x(), mid.y(), mid.z()};
    for (int i = 0; i < 3; i++) {
      double delta = targetComponents[i] - start[i];
      assertTrue(midComponents[i] > start[i] + 0.25 * delta, "past the quarter point");
      assertTrue(midComponents[i] < start[i] + 0.95 * delta, "before the target");
    }

    clock.advance(500);
    runFrame();
    TestUtils.assertVectorEquals(TARGET_POSITION, lastWrittenPosition());
  }

  @Test
  void transitionSkippedWhenIncomingPerspectiveDisallowsIt() {
    manager.transition().setEnabled(true);
    FakeBehavior target = new FakeBehavior("test.target");
    target.position = TARGET_POSITION;
    target.allowTransitionIn = false;
    registerBehavior(target);

    runFrame();

    manager.restoreSelection("test.target");
    runFrame();
    TestUtils.assertVectorEquals(TARGET_POSITION, lastWrittenPosition());
  }

  // endregion

  // region snapshots, fov and projection

  @Test
  void previousCameraStateIsNullBeforeFirstUpdateAndIndependentAfterwards() {
    assertNull(manager.getPreviousCameraState());

    FakeBehavior target = new FakeBehavior("test.target");
    target.position = TARGET_POSITION;
    target.allowTransitionIn = false;
    registerBehavior(target);

    runFrame();
    PerspectiveState first = manager.getPreviousCameraState();
    assertNotNull(first);
    assertFalse(first == manager.getPreviousCameraState());

    manager.restoreSelection("test.target");
    runFrame();

    TestUtils.assertVectorEquals(VANILLA_POSITION, first.position());
    TestUtils.assertVectorEquals(TARGET_POSITION, manager.getPreviousCameraState().position());
  }

  @Test
  void modifyFovCachesOnlyValidVanillaSamplesAndReturnsPipelineFov() {
    FakeBehavior target = new FakeBehavior("test.target");
    target.fovDeg = 100f;
    registerBehavior(target);
    FakeBehavior passive = new FakeBehavior("test.passive");
    passive.allowTransitionIn = false;
    registerBehavior(passive);

    assertEquals(70f, manager.modifyFov(120f));
    manager.restoreSelection("test.target");
    runFrame();
    assertEquals(100f, manager.modifyFov(80f));

    manager.restoreSelection("test.passive");
    runFrame();
    assertEquals(80f, manager.modifyFov(-5f));
    runFrame();
    assertEquals(80f, manager.modifyFov(Float.NaN));
  }

  @Test
  void modifyProjectionReflectsPipelineState() {
    FakeBehavior target = new FakeBehavior("test.target");
    target.orthographic = true;
    target.orthographicHeight = 8f;
    target.allowTransitionIn = false;
    registerBehavior(target);
    runFrame();

    ModifyProjectionContext perspectiveContext = new ModifyProjectionContext();
    perspectiveContext.setup();
    manager.modifyProjection(perspectiveContext);
    assertFalse(perspectiveContext.orthographic);

    manager.restoreSelection("test.target");
    runFrame();
    ModifyProjectionContext orthographicContext = new ModifyProjectionContext();
    orthographicContext.setup();
    manager.modifyProjection(orthographicContext);
    assertTrue(orthographicContext.orthographic);
    assertEquals(8f, orthographicContext.orthographicHeight);
  }

  // endregion

  // region disable and entry guards

  @Test
  void disablingApiDeactivatesCurrentPerspectiveAndInvalidatesSnapshot() {
    FakeBehavior target = new FakeBehavior("test.target");
    registerBehavior(target);
    runFrame();
    assertNotNull(manager.getPreviousCameraState());

    List<String> notified = new ArrayList<>();
    manager.onCurrentChanged((from, to) -> notified.add(to == null ? null : to.info().id()));

    manager.onEnabledChanged(false);

    assertNull(manager.getCurrent());
    assertNull(manager.getPreviousCameraState());
    assertEquals(List.of("test.default:activate", "test.default:deactivate"), defaultBehavior.events);
    assertEquals(1, notified.size());
    assertNull(notified.get(0));

    manager.onEnabledChanged(true);
    assertNull(manager.getCurrent());

    runFrame();
    assertEquals("test.default", manager.getCurrent().info().id());
  }

  @Test
  void updateCameraIgnoresAuxiliaryCamerasAndCamerasWithoutEntity() {
    runFrame();
    cameraOps.writtenPositions.clear();

    manager.updateCamera(0.5f, new Camera());
    manager.updateCamera(0.5f, cameraOps.mainCamera);
    assertThrows(NullPointerException.class, () -> manager.updateCamera(0.5f, null));

    assertTrue(cameraOps.writtenPositions.isEmpty());
    TestUtils.assertVectorEquals(VANILLA_POSITION, manager.getPreviousCameraState().position());
  }

  // endregion

  // region fixtures

  private static final class FakeClock implements DoubleSupplier {
    double nowMs = 1_000.0;

    @Override
    public double getAsDouble() {
      return nowMs;
    }

    void advance(double ms) {
      nowMs += ms;
    }
  }

  private static final class RecordingCameraOperations implements CameraOperations {
    final Camera mainCamera = new Camera();
    final List<Vector3dc> writtenPositions = new ArrayList<>();
    final List<Quaternionfc> writtenRotations = new ArrayList<>();
    final List<CameraType> cameraTypeChanges = new ArrayList<>();
    private CameraType lastCameraType;

    @Override
    public @Nullable Camera getMainCamera() {
      return mainCamera;
    }

    @Override
    public @Nullable Entity getCameraEntity(@NonNull Camera camera) {
      return null;
    }

    @Override
    public @NonNull Vector3d getCameraPosition(@NonNull Camera camera, @NonNull Vector3d dest) {
      return dest.set(VANILLA_POSITION);
    }

    @Override
    public @NonNull Quaternionf getCameraRotation(
        @NonNull Camera camera, @NonNull Quaternionf dest) {
      return dest.set(0, 0, 0, 1);
    }

    @Override
    public void setCameraPosition(@NonNull Camera camera, @NonNull Vector3dc position) {
      writtenPositions.add(new Vector3d(position));
    }

    @Override
    public void setCameraRotation(@NonNull Camera camera, @NonNull Quaternionfc rotation) {
      writtenRotations.add(new Quaternionf(rotation));
    }

    @Override
    public void updateCameraType(@NonNull CameraType cameraType) {
      if (cameraType == lastCameraType) return;
      lastCameraType = cameraType;
      cameraTypeChanges.add(cameraType);
    }
  }

  private record Resolved(Vector3dc position, float fovDeg) {}

  private static final class FakeBehavior implements PerspectiveBehavior {
    final String id;
    List<String> events;
    final List<Resolved> resolvedStates = new ArrayList<>();

    BaseType baseType = BaseType.FIRST_PERSON;
    boolean available = true;
    boolean allowTransitionIn = true;
    boolean allowTransitionOut = true;
    boolean throwOnActivate;
    boolean throwOnBaseType;
    boolean throwOnCompute;
    boolean setNaNFov;
    Float fovDeg;
    Vector3dc position;
    boolean orthographic;
    float orthographicHeight = 8f;

    FakeBehavior(String id) {
      this(id, new ArrayList<>());
    }

    FakeBehavior(String id, List<String> sharedEvents) {
      this.id = id;
      this.events = sharedEvents;
    }

    void logTo(List<String> sharedEvents) {
      this.events = sharedEvents;
    }

    @Override
    public @NonNull BaseType getBaseType() {
      if (throwOnBaseType) throw new IllegalStateException("getBaseType failure");
      return baseType;
    }

    @Override
    public boolean allowsTransitionIn() {
      return allowTransitionIn;
    }

    @Override
    public boolean allowsTransitionOut() {
      return allowTransitionOut;
    }

    @Override
    public boolean isAvailable() {
      return available;
    }

    @Override
    public void onActivate() {
      if (throwOnActivate) throw new IllegalStateException("onActivate failure");
      events.add(id + ":activate");
    }

    @Override
    public void onDeactivate() {
      events.add(id + ":deactivate");
    }

    @Override
    public void computeCameraState(
        PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext context) {
      if (throwOnCompute) throw new IllegalStateException("computeCameraState failure");
      if (position != null) state.position().set(position);
      if (setNaNFov) state.setFovDeg(Float.NaN);
      else if (fovDeg != null) state.setFovDeg(fovDeg);
      if (orthographic) {
        state.setProjectionMode(ProjectionMode.ORTHOGRAPHIC);
        state.setOrthographicHeight(orthographicHeight);
      }
    }

    @Override
    public void afterCameraStateResolved(
        @NonNull PerspectiveState state, @NonNull PerspectiveContext context) {
      resolvedStates.add(new Resolved(new Vector3d(state.position()), state.getFovDeg()));
    }
  }

  // endregion
}
