package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.CameraSpace;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveModifierChainImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveOverrideChainImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.impl.TransitionImpl;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveContextImpl;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.wheel.WheelSwitcherBehavior;
import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the lifecycle and state of camera perspectives.
public final class PerspectiveManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveManager.class);
  public static final PerspectiveManager INSTANCE =
      new PerspectiveManager(new WheelSwitcherBehavior(PerspectiveRegistryImpl.INSTANCE));

  private final Sanitizer.ThrottledAction throttledAction = new Sanitizer.ThrottledAction(5000);

  // region components

  private final TransitionImpl transition;
  private final PerspectiveModifierChainImpl modifiers;
  private final PerspectiveOverrideChainImpl overrides;
  private final PerspectiveSwitcherManagerImpl switchers;

  public @NonNull Transition transition() {
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
  private volatile @Nullable Perspective current;
  private volatile @Nullable PerspectiveBehavior currentBehavior;
  private volatile @Nullable PerspectiveBehavior previousBehavior = null;

  // endregion

  // region temp states

  private boolean isTempStateInited = false;
  private final Vector3d tempPosition = new Vector3d();

  /// Use API convention
  private final Quaternionf tempRotation = new Quaternionf();
  private final Quaternionf tempRotationMcQuat =
      CameraSpace.apiToMc(tempRotation, new Quaternionf());
  private float tempFov = 70;

  // endregion

  private PerspectiveManager(@NonNull PerspectiveSwitcherBehavior defaultSwitcher) {
    switchers = new PerspectiveSwitcherManagerImpl(defaultSwitcher);

    overrides = new PerspectiveOverrideChainImpl(PerspectiveRegistryImpl.INSTANCE);
    overrides.push(PerspectiveSwitcherManagerImpl.KEY, Integer.MIN_VALUE, switchers);

    modifiers =
        new PerspectiveModifierChainImpl(
            (modifier, e) -> reportException(modifier.id(), "applyTransform", e),
            (modifier, msg) ->
                throttledAction.run(
                    modifier.id() + ":applyFov:invalid", () -> LOGGER.warn("{}", msg)));

    transition = new TransitionImpl();
  }

  // region perspective management

  public @NonNull Perspective getCurrent() {
    var current = this.current;
    if (current == null) {
      current = PerspectiveRegistryImpl.INSTANCE.getDefault();
      this.current = current;
    }

    return current;
  }

  public void clientTick(Minecraft minecraft) {
    // tick switchers
    switchers.clientTick(minecraft);

    // Resolve current id from override chain
    Perspective resolved = PerspectiveRegistryImpl.INSTANCE.getOrDefault(overrides.get());
    current = resolved;

    PerspectiveBehavior resolvedBehavior =
        PerspectiveRegistryImpl.INSTANCE.getBehaviorOrDefault(resolved.id());

    // If the current perspective changed
    var currentPerspective = this.currentBehavior;
    if (resolvedBehavior != currentPerspective) {
      if (currentPerspective != null) {
        currentPerspective.onDeactivate();
      }
      previousBehavior = currentPerspective;
      this.currentBehavior = resolvedBehavior;

      resolvedBehavior.onActivate();

      Bridge.updateCameraType(resolved.cameraType());

      startTransition();
    }

    // Run perspective client tick
    try {
      resolvedBehavior.clientTickWhenActive(minecraft);
    } catch (Throwable e) {
      reportException(resolved.id(), "clientTick", e);
    }
  }

  // endregion

  // region camera update

  private final PerspectiveContextImpl renderTickContext = new PerspectiveContextImpl();
  private final Vector3d backupPosition = new Vector3d();
  private final Quaternionf backupRotation = new Quaternionf();

  /// Updates camera position and rotation based on the current perspective.
  ///
  /// @param partialTicks interpolation factor between ticks
  /// @param camera the camera to update
  public void updateCamera(float partialTicks, Camera camera) {
    Entity entity = CameraAccessor.of(camera).getEntity();
    if (entity == null) {
      LOGGER.warn("Somehow camera entity is null");
      return;
    }

    Perspective current = this.current;
    PerspectiveBehavior currentBehavior = this.currentBehavior;
    PerspectiveBehavior previousBehavior = this.previousBehavior;

    if (current == null || currentBehavior == null) {
      return;
    }

    double now = TransitionImpl.getTimeMs();

    boolean isTransitioning =
        transition.isInTransition(now)
            && currentBehavior.allowTransitionIn()
            && (previousBehavior == null || previousBehavior.allowTransitionOut());

    // Setup context object
    renderTickContext.setup(partialTicks, entity, isTransitioning);

    // Event: render tick
    try {
      currentBehavior.renderTickWhenActive(renderTickContext);
    } catch (Throwable e) {
      reportException(current.id(), "renderTick", e);
    }

    // Extract current vanilla state and backup
    Bridge.getCameraPosition(camera, tempPosition);
    Bridge.getCameraRotation(camera, tempRotationMcQuat);
    CameraSpace.mcToApi(tempRotationMcQuat, tempRotation);

    backupPosition.set(tempPosition);
    backupRotation.set(tempRotation);

    // 1. Apply Base PerspectiveBehavior
    try {
      currentBehavior.applyTransform(renderTickContext, tempPosition, tempRotation);
    } catch (Throwable e) {
      reportException(current.id(), "applyTransform", e);
    }

    // 2. Apply Modifiers (Further mutates the target state BEFORE transition)
    modifiers.applyTransform(renderTickContext, tempPosition, tempRotation);

    // Sanitize and fallback if needed
    boolean posInvalid = !Sanitizer.isFinite(tempPosition);
    boolean rotInvalid = !Sanitizer.isFinite(tempRotation);
    if (posInvalid || rotInvalid) {
      throttledAction.run(
          current.id() + ":applyTransform:invalid",
          () ->
              LOGGER.warn(
                  "PerspectiveBehavior '{}' provided invalid state during applyTransform. Falling back to vanilla. pos: {}, rot: {}",
                  current.id(),
                  tempPosition,
                  tempRotation));

      if (posInvalid) {
        tempPosition.set(backupPosition);
      }
      if (rotInvalid) {
        tempRotation.set(backupRotation);
      }
    }

    // Apply transition
    if (isTransitioning) {
      transition.updateTransform(now, tempPosition, tempRotation, tempPosition, tempRotation);

      if (!Sanitizer.isFinite(tempPosition)) {
        throttledAction.run(
            "transition:position",
            () ->
                LOGGER.warn(
                    "Transition position is invalid, falling back to vanilla. pos: {}",
                    tempPosition));
        tempPosition.set(backupPosition);
      }
      if (!Sanitizer.isFinite(tempRotation)) {
        throttledAction.run(
            "transition:rotation",
            () ->
                LOGGER.warn(
                    "Transition rotation is invalid, falling back to vanilla. rot: {}",
                    tempRotation));
        tempRotation.set(backupRotation);
      }
    }
    isTempStateInited = true;

    // Commit to camera
    Bridge.setCameraPosition(camera, tempPosition);
    Bridge.setCameraRotation(camera, CameraSpace.apiToMc(tempRotation, tempRotationMcQuat));
  }

  /// Called by ModEvents during MODIFY_FIELD_OF_VIEW.
  public float modifyFov(float vanillaFov) {
    float fov = vanillaFov;

    Perspective current = this.current;
    PerspectiveBehavior currentBehavior = this.currentBehavior;
    PerspectiveBehavior previousBehavior = this.previousBehavior;

    if (current == null || currentBehavior == null) {
      return tempFov = fov;
    }

    // Apply Base PerspectiveBehavior
    try {
      fov = currentBehavior.applyFov(renderTickContext, vanillaFov);
    } catch (Throwable e) {
      reportException(current.id(), "applyFov", e);
      fov = vanillaFov;
    }
    // Sanitize
    boolean fovInvalid = !Sanitizer.isFinite(fov) || fov < 0.0f || fov > 180.0f;
    if (fovInvalid) {
      fov = vanillaFov;
      throttledAction.run(
          current.id() + ":applyFov:invalid",
          () ->
              LOGGER.warn(
                  "PerspectiveBehavior '{}' returned invalid FOV during applyFov. Falling back to vanilla.",
                  current.id()));
    }

    // Apply Modifiers
    fov = modifiers.applyFov(renderTickContext, fov);

    // Apply Transition
    double now = TransitionImpl.getTimeMs();
    if (transition.isInTransition(now)
        && currentBehavior.allowTransitionIn()
        && (previousBehavior == null || previousBehavior.allowTransitionOut())) {
      tempFov = transition.updateFov(now, fov);
      if (!Sanitizer.isFinite(tempFov)) {
        throttledAction.run(
            "transition:fov",
            () ->
                LOGGER.warn(
                    "Transition FOV is invalid, falling back to vanilla. fov: {}", tempFov));
        tempFov = fov;
      }
    } else {
      tempFov = fov;
    }
    return tempFov;
  }

  // endregion

  public void setCurrent(@NonNull Perspective perspective) {
    current = perspective;
  }

  private void startTransition() {
    if (!isTempStateInited) {
      Camera camera = Bridge.getMainCamera();
      if (camera != null) {
        Bridge.getCameraPosition(camera, tempPosition);
        Bridge.getCameraRotation(camera, tempRotationMcQuat);
        CameraSpace.mcToApi(tempRotationMcQuat, tempRotation);
        isTempStateInited = true;
      }
    }
    transition.setStartState(TransitionImpl.getTimeMs(), tempPosition, tempRotation, tempFov);
  }

  private void reportException(String id, String phase, Throwable throwable) {
    throttledAction.run(
        id + ":" + phase + ":exception",
        () -> LOGGER.warn("'{}' threw an exception during {}.", id, phase, throwable));
  }
}
