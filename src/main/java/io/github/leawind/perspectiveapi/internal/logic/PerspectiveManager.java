package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
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
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveStateImpl;
import io.github.leawind.perspectiveapi.internal.impl.ThrottledPerspectiveSanitizer;
import io.github.leawind.perspectiveapi.internal.impl.TransitionImpl;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveContextImpl;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.wheel.WheelSwitcherBehavior;
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
      INSTANCE =
          new PerspectiveManager(new WheelSwitcherBehavior(PerspectiveRegistryImpl.INSTANCE));
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
  private volatile @Nullable Perspective current = null;
  private volatile @Nullable PerspectiveBehavior currentBehavior = null;
  private volatile @Nullable PerspectiveBehavior previousBehavior = null;

  // endregion

  // region camera state

  private boolean isTempStateInited = false;
  private final Quaternionf tempMcQuat = new Quaternionf();

  private final PerspectiveStateImpl targetState = new PerspectiveStateImpl();
  private final PerspectiveStateImpl backupState = new PerspectiveStateImpl();
  private float cachedVanillaFovDeg = PerspectiveStateImpl.DEFAULT_FOV_DEGREES;

  // endregion

  private PerspectiveManager(@NonNull PerspectiveSwitcherBehavior defaultSwitcher) {
    switchers = new PerspectiveSwitcherManagerImpl(defaultSwitcher);

    overrides = new PerspectiveOverrideChainImpl(PerspectiveRegistryImpl.INSTANCE);
    overrides.push(PerspectiveSwitcherManagerImpl.KEY, Integer.MIN_VALUE, switchers);

    modifiers = new PerspectiveModifierChainImpl(sanitizer);

    transition = new TransitionImpl();
  }

  // region perspective management

  /// @throws IllegalStateException if called before SPI discovery completes during mod loading
  public @NonNull Perspective getCurrent() throws IllegalStateException {
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
        extensions.run(
            currentPerspective.getClass().getName(),
            "onDeactivate",
            currentPerspective::onDeactivate);
      }
      previousBehavior = currentPerspective;
      this.currentBehavior = resolvedBehavior;

      extensions.run(resolved.id(), "onActivate", resolvedBehavior::onActivate);

      Bridge.updateCameraType(
          switch (resolved.baseType()) {
            case FIRST_PERSON -> CameraType.FIRST_PERSON;
            case THIRD_PERSON_BACK -> CameraType.THIRD_PERSON_BACK;
            case THIRD_PERSON_FRONT -> CameraType.THIRD_PERSON_FRONT;
          });

      startTransition();
    }

    // Run perspective client tick
    extensions.run(
        resolved.id(), "clientTick", () -> resolvedBehavior.clientTickWhenActive(minecraft));
  }

  // endregion

  // region camera update

  private final PerspectiveContextImpl renderTickContext = new PerspectiveContextImpl();

  /// Updates camera position, rotation, and FOV based on the current perspective.
  ///
  /// ### Steps
  ///
  /// 1. Prepare and validate context — return early if context invalid
  /// 2. Call `preApplyWhenActive` callback
  /// 3. Apply base perspective to target state via `applyCameraState`
  /// 4. Sanitize target state, fallback if invalid
  /// 5. Apply modifier chain to target state via `applyCameraState`.
  ///    Sanitize after applying each modifier
  /// 6. Apply transition interpolation if transitioning, and sanitize
  /// 7. Write final state to camera
  /// 8. Call `postApplyWhenActive` callback
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
      PerspectiveBehavior previousBehavior = this.previousBehavior;

      if (current == null || currentBehavior == null) {
        return;
      }
      now = TransitionImpl.getTimeMs();
      isTransitioning =
          transition.isInTransition(now)
              && allowsTransition(current.id(), currentBehavior, true)
              && (previousBehavior == null
                  || allowsTransition(
                      previousBehavior.getClass().getName(), previousBehavior, false));

      renderTickContext.setup(partialTicks, entity, isTransitioning);
    }

    // Pre-apply callback
    extensions.run(
        current.id(), "preApply", () -> currentBehavior.preApplyWhenActive(renderTickContext));

    // Backup vanilla state for fallback
    {
      Bridge.getCameraPosition(camera, targetState.position());
      Bridge.getCameraRotation(camera, tempMcQuat);
      CameraSpace.mcToApi(tempMcQuat, targetState.rotation());
      targetState.setFovDeg(cachedVanillaFovDeg);
      backupState.set(targetState);
    }

    // Apply perspective
    if (!extensions.run(
        current.id(),
        "apply",
        () -> currentBehavior.applyCameraState(targetState, renderTickContext))) {
      targetState.set(backupState);
    }

    // Sanitize
    sanitizer.sanitize(
        current.id(),
        targetState,
        backupState,
        () -> "Perspective '" + current.id() + "' provided invalid state");

    // Apply modifiers
    modifiers.applyCameraState(targetState, renderTickContext);

    // Transition interpolation
    if (isTransitioning) {
      transition.update(now, targetState, targetState);
      sanitizer.sanitize(
          "transition", targetState, backupState, () -> "Transition produced invalid state");
    }

    isTempStateInited = true;

    // Write to camera
    Bridge.setCameraPosition(camera, targetState.position());
    Bridge.setCameraRotation(camera, CameraSpace.apiToMc(targetState.rotation(), tempMcQuat));

    // Post-apply callback
    extensions.run(
        current.id(),
        "postApply",
        () -> currentBehavior.postApplyWhenActive(targetState, renderTickContext));
  }

  /// Called by ModEvents during MODIFY_FIELD_OF_VIEW.
  ///
  /// Updates the cached vanilla FOV for the next frame and returns the
  /// already-computed FOV from the current frame's camera state pipeline.
  public float modifyFov(float vanillaFovDeg) {
    cachedVanillaFovDeg = vanillaFovDeg;
    return targetState.getFovDeg();
  }

  // endregion

  public void setCurrent(@NonNull Perspective perspective) {
    current = perspective;
  }

  private void startTransition() {
    if (!isTempStateInited) {
      Camera camera = Bridge.getMainCamera();
      if (camera != null) {
        Bridge.getCameraPosition(camera, targetState.position());
        Bridge.getCameraRotation(camera, tempMcQuat);
        CameraSpace.mcToApi(tempMcQuat, targetState.rotation());
        isTempStateInited = true;
      }
    }
    transition.setStartState(TransitionImpl.getTimeMs(), targetState);
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
