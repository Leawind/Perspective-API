package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveAPIRuntime;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.PerspectiveSelection;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
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
import io.github.leawind.perspectiveapi.internal.logic.builtin.selection.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
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
    PerspectiveAPIRuntime.install(PerspectiveAPIRuntimeImpl.INSTANCE);
    try {
      INSTANCE = new PerspectiveManager();
    } catch (Throwable e) {
      LOGGER.error("Failed to initialize PerspectiveManager", e);
      throw e;
    }
  }

  private final Sanitizer.ThrottledAction throttledAction = new Sanitizer.ThrottledAction(5000);
  private final ExtensionInvoker extensions = new ExtensionInvoker(LOGGER, "Perspective");

  private final ThrottledPerspectiveSanitizer sanitizer =
      new ThrottledPerspectiveSanitizer(throttledAction);
  // region components

  private final TransitionImpl transition;
  private final PerspectiveModifierChainImpl modifiers;
  private final PerspectiveOverrideChainImpl overrides;
  private final PerspectiveSelectionImpl selection;
  private final PerspectiveSwitcher perspectiveSwitcher;

  public @NonNull TransitionImpl transition() {
    return transition;
  }

  public @NonNull PerspectiveModifierChain modifiers() {
    return modifiers;
  }

  public @NonNull PerspectiveOverrideChainImpl overrides() {
    return overrides;
  }

  public @NonNull PerspectiveSelection selection() {
    return selection;
  }

  public @Nullable String getSelected() {
    return selection.get();
  }

  public void restoreSelection(@Nullable String perspectiveId) {
    synchronized (selection) {
      selection.set(perspectiveId);
    }
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

  private PerspectiveManager() {
    selection = new PerspectiveSelectionImpl();
    perspectiveSwitcher = PerspectiveSwitcher.INSTANCE;
    perspectiveSwitcher.init();

    overrides = new PerspectiveOverrideChainImpl(PerspectiveRegistryImpl.INSTANCE);

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

  public void onEnabledChanged(boolean enabled) {
    if (enabled) return;

    perspectiveSwitcher.deactivate();
    PerspectiveBehavior deactivated = currentBehavior;
    currentBehavior = null;
    transitionAllowed = false;
    isTransitionStartStateInitialized = false;
    hasPreviousCameraState = false;
    if (deactivated != null) {
      extensions.run(deactivated.getClass().getName(), "onDeactivate", deactivated::onDeactivate);
    }
  }

  /// Resolves the current perspective and applies the base type it currently reports, before
  /// vanilla reads {@link CameraType} to update the main camera for a render frame.
  void beforeMainCameraUpdate() {
    updateCurrentPerspective();
  }

  private void updateCurrentPerspective() {
    // Resolve temporary overrides before the persistent selection.
    Perspective previous = current;
    String resolvedId = overrides.get();
    if (resolvedId == null) resolvedId = selection.get();
    Perspective resolved = resolveAvailableOrDefault(resolvedId);
    PerspectiveBehavior resolvedBehavior =
        PerspectiveRegistryImpl.INSTANCE.getBehaviorOrDefault(resolved.info().id());

    // If the current perspective changed
    PerspectiveBehavior previousBehavior = currentBehavior;
    if (resolved != previous || previousBehavior == null) {
      boolean outgoingAllowsTransition =
          previous != null
              && previousBehavior != null
              && allowsTransition(previous.info().id(), previousBehavior, false);
      if (previousBehavior != null) {
        extensions.run(
            previousBehavior.getClass().getName(), "onDeactivate", previousBehavior::onDeactivate);
      }
      current = resolved;
      this.currentBehavior = resolvedBehavior;

      extensions.run(resolved.info().id(), "onActivate", resolvedBehavior::onActivate);
      boolean incomingAllowsTransition =
          previousBehavior != null
              && allowsTransition(resolved.info().id(), resolvedBehavior, true);
      transitionAllowed = outgoingAllowsTransition && incomingAllowsTransition;
      startTransition();
    }

    applyReportedBaseType(resolved.info().id(), resolvedBehavior);
  }

  /// Applies the base type currently reported by the given perspective.
  ///
  /// Bridge keeps the vanilla camera type unchanged when the reported value did not change. A
  /// failure is logged and the current vanilla camera type is kept for that evaluation.
  private void applyReportedBaseType(@NonNull String id, @NonNull PerspectiveBehavior behavior) {
    BaseType baseType = extensions.callOrElse(id, "getBaseType", behavior::getBaseType, null);
    if (baseType != null) updateCameraType(baseType);
  }

  private @NonNull Perspective resolveAvailableOrDefault(@Nullable String perspectiveId) {
    if (perspectiveId != null) {
      Perspective perspective = PerspectiveRegistryImpl.INSTANCE.get(perspectiveId);
      if (perspective != null && perspective.isAvailable()) return perspective;
    }
    return PerspectiveRegistryImpl.INSTANCE.getDefault();
  }

  private static void updateCameraType(@NonNull BaseType baseType) {
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

  /// Updates the main camera's transform and projection settings based on the current perspective.
  ///
  /// Calls for auxiliary cameras are ignored.
  ///
  /// ### Steps
  ///
  /// 1. Prepare the frame context and read the vanilla camera state
  /// 2. Apply the active perspective and sanitize its target state
  /// 3. Snapshot the perspective result for modifiers
  /// 4. Apply and sanitize modifiers
  /// 5. Apply and sanitize perspective-switch transition interpolation
  /// 6. Write the final state to the camera
  /// 7. Call {@link PerspectiveBehavior#afterCameraStateResolved}
  /// 8. Publish an independent source for previous-state snapshots and future transitions
  ///
  /// @param partialTicks interpolation factor between ticks
  /// @param camera the camera to update
  public void updateCamera(float partialTicks, @NonNull Camera camera) {
    Objects.requireNonNull(camera);
    if (Bridge.getMainCamera() != camera) return;

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
        "computeCameraState",
        () -> currentBehavior.computeCameraState(targetState, renderTickContext))) {
      targetState.set(backupState);
    }

    // Sanitize
    sanitizer.sanitize(
        current.info().id(),
        targetState,
        backupState,
        () -> "Perspective '" + current.info().id() + "' provided invalid state");

    renderTickContext.setPerspectiveBaseState(new PerspectiveStateSnapshot(targetState));

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
        "afterCameraStateResolved",
        () -> currentBehavior.afterCameraStateResolved(backupState, renderTickContext));

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
    context.orthographic = targetState.getProjectionMode() == ProjectionMode.ORTHOGRAPHIC;
    context.orthographicHeight = targetState.getOrthographicHeight();
  }

  // endregion

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
        incoming ? "allowsTransitionIn" : "allowsTransitionOut",
        incoming ? behavior::allowsTransitionIn : behavior::allowsTransitionOut,
        false);
  }
}
