package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.api.ProjectionMode;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.CameraSpace;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyProjectionContext;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveModifierChainImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveOverrideChainImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveStateImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveStateSnapshot;
import io.github.leawind.perspectiveapi.internal.impl.ThrottledPerspectiveSanitizer;
import io.github.leawind.perspectiveapi.internal.impl.TransitionImpl;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveContextImpl;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.OrbitSwitcherBehavior;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the lifecycle and state of camera perspectives.
public final class PerspectiveManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveManager.class);
  public static final PerspectiveManager INSTANCE;

  static {
    PerspectiveAPI.installRuntime(PerspectiveAPIRuntimeImpl.INSTANCE);
    try {
      INSTANCE = new PerspectiveManager(OrbitSwitcherBehavior.INSTANCE);
    } catch (Throwable e) {
      LOGGER.error("Failed to initialize PerspectiveManager", e);
      throw e;
    }
  }

  private final Sanitizer.ThrottledAction throttledAction = new Sanitizer.ThrottledAction(5000);
  private final ExtensionInvoker extensions = new ExtensionInvoker(LOGGER, "Perspective");

  private final ThrottledPerspectiveSanitizer sanitizer =
      new ThrottledPerspectiveSanitizer(throttledAction);
  private final LogicUpdateScheduler logicUpdateScheduler = new LogicUpdateScheduler();
  private volatile boolean registryDirty;

  // region components

  private final TransitionImpl transition;
  private final PerspectiveModifierChainImpl modifiers;
  private final PerspectiveOverrideChainImpl overrides;
  private final PerspectiveSwitcherManagerImpl switchers;

  public @NonNull TransitionImpl transition() {
    return transition;
  }

  public @NonNull PerspectiveModifierChain modifiers() {
    return modifiers;
  }

  public @NonNull PerspectiveOverrideChainImpl overrides() {
    return overrides;
  }

  public @NonNull PerspectiveSwitcherManagerImpl switchers() {
    return switchers;
  }

  // endregion

  // region current state
  private volatile @Nullable Perspective current = null;
  private volatile @Nullable PerspectiveBehavior currentBehavior = null;
  private volatile boolean transitionAllowed;

  // endregion

  // region camera state

  private boolean isTransitionStartStateInitialized;
  private boolean hasPreviousCameraState;
  private final Quaternionf tempMcQuat = new Quaternionf();

  private final PerspectiveStateImpl targetState = new PerspectiveStateImpl();
  private final PerspectiveStateImpl backupState = new PerspectiveStateImpl();
  private final PerspectiveStateImpl lastAppliedState = new PerspectiveStateImpl();
  private float cachedVanillaFovDeg = PerspectiveStateImpl.DEFAULT_FOV_DEGREES;

  // endregion

  private PerspectiveManager(@NonNull PerspectiveSwitcherBehavior defaultSwitcher) {
    switchers = new PerspectiveSwitcherManagerImpl(defaultSwitcher);
    PerspectiveRegistryImpl.INSTANCE.onUpdate().on(() -> registryDirty = true);

    overrides = new PerspectiveOverrideChainImpl(PerspectiveRegistryImpl.INSTANCE);
    overrides.register(Integer.MIN_VALUE, switchers);

    modifiers = new PerspectiveModifierChainImpl(sanitizer);

    transition = new TransitionImpl();
  }

  // region perspective management

  /// Returns the perspective that currently owns the camera, or `null` when none is active.
  public @Nullable Perspective getCurrent() {
    return currentBehavior == null ? null : current;
  }

  /// Returns an independent snapshot of the last completed main-camera update.
  public @Nullable PerspectiveState getPreviousCameraState() {
    return hasPreviousCameraState ? new PerspectiveStateSnapshot(lastAppliedState) : null;
  }

  /// Returns the last resolved perspective, using the default before initial resolution.
  public @NonNull Perspective getLastResolvedOrDefault() {
    Perspective current = this.current;
    if (current != null) return current;
    current = PerspectiveRegistryImpl.INSTANCE.getDefault();
    this.current = current;
    return current;
  }

  public void clientTick(Minecraft minecraft) {
    PerspectiveRegistryImpl.INSTANCE.beginAvailabilitySnapshot();

    // tick switchers
    switchers.clientTick(minecraft);

    if (registryDirty) {
      registryDirty = false;
      logicUpdateScheduler.reset();
      updateCurrentPerspective(true);
    } else if (logicUpdateScheduler.tick(PerspectiveAPI.getLogicTickInterval())) {
      updateCurrentPerspective(false);
    }

    Perspective current = this.current;
    PerspectiveBehavior currentBehavior = this.currentBehavior;
    if (current == null || currentBehavior == null) return;

    // Run perspective client tick
    extensions.run(
        current.info().id(), "clientTick", () -> currentBehavior.clientTickWhenActive(minecraft));
  }

  void resetLogicUpdateScheduler() {
    logicUpdateScheduler.reset();
  }

  public void onEnabledChanged(boolean enabled) {
    logicUpdateScheduler.reset();
    if (enabled) {
      registryDirty = true;
      return;
    }

    PerspectiveBehavior deactivated = currentBehavior;
    currentBehavior = null;
    transitionAllowed = false;
    isTransitionStartStateInitialized = false;
    hasPreviousCameraState = false;
    if (deactivated != null) {
      extensions.run(deactivated.getClass().getName(), "onDeactivate", deactivated::onDeactivate);
    }
  }

  private void updateCurrentPerspective(boolean registryChanged) {
    // Resolve current id from override chain
    Perspective previous = current;
    Perspective resolved = PerspectiveRegistryImpl.INSTANCE.getOrDefault(overrides.get());
    PerspectiveBehavior resolvedBehavior =
        PerspectiveRegistryImpl.INSTANCE.getBehaviorOrDefault(resolved.info().id());

    // If the current perspective changed
    PerspectiveBehavior previousBehavior = currentBehavior;
    if (resolved != previous || previousBehavior == null) {
      boolean outgoingAllowsTransition =
          previousBehavior != null
              && allowsTransition(previous.info().id(), previousBehavior, false);
      if (previousBehavior != null) {
        extensions.run(
            previousBehavior.getClass().getName(), "onDeactivate", previousBehavior::onDeactivate);
      }
      current = resolved;
      this.currentBehavior = resolvedBehavior;

      updateCameraType(resolved.info().baseType());
      extensions.run(resolved.info().id(), "onActivate", resolvedBehavior::onActivate);
      boolean incomingAllowsTransition =
          previousBehavior != null
              && allowsTransition(resolved.info().id(), resolvedBehavior, true);
      transitionAllowed = outgoingAllowsTransition && incomingAllowsTransition;
      startTransition();
    } else if (registryChanged) {
      updateCameraType(resolved.info().baseType());
    }
  }

  private static void updateCameraType(PerspectiveBehavior.@NonNull BaseType baseType) {
    Bridge.updateCameraType(
        switch (baseType) {
          case FIRST_PERSON -> CameraType.FIRST_PERSON;
          case THIRD_PERSON_BACK -> CameraType.THIRD_PERSON_BACK;
          case THIRD_PERSON_FRONT -> CameraType.THIRD_PERSON_FRONT;
        });
  }

  // endregion

  // region camera update

  private final PerspectiveContextImpl renderTickContext = new PerspectiveContextImpl();

  /// Updates camera transform and projection settings based on the current perspective.
  ///
  /// ### Steps
  ///
  /// 1. Prepare the frame context and read the vanilla camera state
  /// 2. Apply the active perspective and sanitize its target state
  /// 3. Apply and sanitize modifiers
  /// 4. Apply and sanitize perspective-switch transition interpolation
  /// 5. Write the final state to the camera
  /// 6. Call {@link PerspectiveBehavior#afterApplyCameraState}
  /// 7. Publish an independent source for previous-state snapshots and future transitions
  ///
  /// @param partialTicks interpolation factor between ticks
  /// @param camera the camera to update
  public void updateCamera(float partialTicks, Camera camera) {
    // Prepare and validate context
    Entity entity;
    Perspective current;
    PerspectiveBehavior currentBehavior;
    double now;
    boolean isTransitioning;
    {
      entity = CameraAccessor.of(camera).getEntity();
      if (entity == null) {
        LOGGER.warn("Somehow camera entity is null");
        return;
      }
      current = this.current;
      currentBehavior = this.currentBehavior;

      if (current == null || currentBehavior == null) {
        return;
      }
      now = TransitionImpl.getTimeMs();
      isTransitioning = transitionAllowed && transition.isInTransition(now);

      renderTickContext.setup(partialTicks, entity, isTransitioning);
    }

    // Backup vanilla state for fallback
    captureVanillaState(camera, targetState);
    backupState.set(targetState);

    // Apply perspective
    if (!extensions.run(
        current.info().id(),
        "apply",
        () -> currentBehavior.applyCameraState(targetState, renderTickContext))) {
      targetState.set(backupState);
    }

    // Sanitize
    sanitizer.sanitize(
        current.info().id(),
        targetState,
        backupState,
        () -> "Perspective '" + current.info().id() + "' provided invalid state");

    // Apply modifiers to the perspective target state
    modifiers.applyCameraState(targetState, renderTickContext);

    // Transition interpolation
    if (isTransitioning) {
      transition.update(now, targetState, targetState);
      sanitizer.sanitize(
          "transition", targetState, backupState, () -> "Transition produced invalid state");
    }

    // Write to camera
    Bridge.setCameraPosition(camera, targetState.position());
    Bridge.setCameraRotation(camera, CameraSpace.apiToMc(targetState.rotation(), tempMcQuat));

    // Post-apply callback
    backupState.set(targetState);
    extensions.run(
        current.info().id(),
        "postApply",
        () -> currentBehavior.afterApplyCameraState(backupState, renderTickContext));

    if (PerspectiveAPI.isEnabled()) {
      lastAppliedState.set(targetState);
      isTransitionStartStateInitialized = true;
      hasPreviousCameraState = true;
    }
  }

  /// Called by ModEvents during MODIFY_FIELD_OF_VIEW.
  ///
  /// Captures a valid vanilla FOV for the next frame and returns the already-computed FOV from the
  /// current frame's camera state pipeline. Invalid vanilla values are ignored so the next frame's
  /// base state and fallback continue to satisfy the API contract.
  public float modifyFov(float vanillaFovDeg) {
    if (ThrottledPerspectiveSanitizer.isValidFovDeg(vanillaFovDeg)) {
      cachedVanillaFovDeg = vanillaFovDeg;
    }
    return targetState.getFovDeg();
  }

  /// Writes the already-computed projection settings into a bridge context.
  public void modifyProjection(@NonNull ModifyProjectionContext context) {
    context.orthographic = targetState.projectionMode() == ProjectionMode.ORTHOGRAPHIC;
    context.orthographicHeight = targetState.getOrthographicHeight();
  }

  // endregion

  public void restoreLastResolved(@NonNull Perspective perspective) {
    current = perspective;
  }

  private void startTransition() {
    if (!isTransitionStartStateInitialized) {
      Camera camera = Bridge.getMainCamera();
      if (camera != null) {
        captureVanillaState(camera, lastAppliedState);
      } else {
        lastAppliedState.set(targetState);
      }
      isTransitionStartStateInitialized = true;
    }
    transition.setStartState(TransitionImpl.getTimeMs(), lastAppliedState);
  }

  private void captureVanillaState(
      @NonNull Camera camera, @NonNull PerspectiveStateImpl destination) {
    Bridge.getCameraPosition(camera, destination.position());
    Bridge.getCameraRotation(camera, tempMcQuat);
    CameraSpace.mcToApi(tempMcQuat, destination.rotation());
    destination.setFovDeg(cachedVanillaFovDeg);
    destination.setProjectionMode(ProjectionMode.PERSPECTIVE);
    destination.setOrthographicHeight(PerspectiveStateImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT);
  }

  private boolean allowsTransition(
      @NonNull String id, @NonNull PerspectiveBehavior behavior, boolean incoming) {
    return extensions.testOrElse(
        id,
        incoming ? "allowTransitionIn" : "allowTransitionOut",
        incoming ? behavior::allowTransitionIn : behavior::allowTransitionOut,
        false);
  }
}
