package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveModifier;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.TransitionController;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveModifierChainImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveOverrideChainImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveWheelImpl;
import io.github.leawind.perspectiveapi.internal.impl.Transition;
import io.github.leawind.perspectiveapi.internal.impl.TransitionImpl;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveContextImpl;
import io.github.leawind.perspectiveapi.internal.logic.builtin.VanillaPerspective;
import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
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
      new PerspectiveManager(VanillaPerspective.FIRST_PERSON);

  private final Sanitizer.ThrottledAction throttledAction = new Sanitizer.ThrottledAction(5000);

  private volatile @NonNull Identifier currentId;

  /// Updated on client tick
  private volatile @NonNull Perspective currentPerspective;
  private volatile @Nullable Perspective previousPerspective = null;

  public final SimpleEventEmitter.Owned<Perspective> onCurrentPerspectiveChanged =
      SimpleEventEmitter.create();

  // region temp states

  private boolean isTempStateInited = false;
  private final Vector3d tempPosition = new Vector3d();
  private final Quaternionf tempRotation = new Quaternionf();
  private float tempFov = 70;

  // endregion

  private void reportException(
      @NonNull PerspectiveModifier modifier, String phase, Throwable throwable) {
    String id = modifier.id().toString();
    throttledAction.run(
        id + ":" + phase + ":exception",
        () -> LOGGER.warn("'{}' threw an exception during {}.", id, phase, throwable));
  }

  private PerspectiveManager(@NonNull Perspective defaultPerspective) {
    Objects.requireNonNull(defaultPerspective);
    registry = new PerspectiveRegistryImpl(defaultPerspective);
    wheel = new PerspectiveWheelImpl(registry);
    modifiers =
        new PerspectiveModifierChainImpl(
            (modifier, e) -> reportException(modifier, "applyTransform", e),
            (modifier, msg) ->
                throttledAction.run(
                    modifier.id() + ":applyFov:invalid", () -> LOGGER.warn("{}", msg)));
    currentId = defaultPerspective.id();
    currentPerspective = defaultPerspective;

    overrides = new PerspectiveOverrideChainImpl();
    overrides.setValidator(registry::contains);
    overrides.push(PerspectiveWheelImpl.KEY, Integer.MIN_VALUE, wheel);

    onCurrentPerspectiveChanged.on(
        () -> {
          if (!isTempStateInited) {
            Camera camera = Bridge.getMainCamera();
            if (camera != null) {
              Bridge.getCameraPosition(camera, tempPosition);
              Bridge.getCameraRotationQuat(camera, tempRotation);
              tempFov = 70.0f;
              isTempStateInited = true;
            }
          }
          transition.setStartState(Transition.getTimeMs(), tempPosition, tempRotation, tempFov);
        });
  }

  // region components
  private final PerspectiveRegistryImpl registry;
  private final PerspectiveModifierChainImpl modifiers;
  private final PerspectiveOverrideChainImpl overrides;
  private final PerspectiveWheelImpl wheel;
  private final Transition transition = new TransitionImpl();

  /// @return The perspective registry.
  public @NonNull PerspectiveRegistry registry() {
    return registry;
  }

  /// @return The transition controller.
  public @NonNull TransitionController transition() {
    return transition;
  }

  /// @return The modifier chain for registering camera modifiers.
  public @NonNull PerspectiveModifierChain modifiers() {
    return modifiers;
  }

  /// @return The override chain controller.
  public @NonNull PerspectiveOverrideChain overrides() {
    return overrides;
  }

  /// @return The perspective cycler for cycling through perspectives.
  public @NonNull PerspectiveWheelImpl wheel() {
    return wheel;
  }

  // endregion

  // region perspective management

  /// Returns the current active perspective after resolving the override chain.
  /// Never returns `null`.
  public @NonNull Perspective getCurrent() {
    return currentPerspective;
  }

  public void clientTick(Minecraft minecraft) {
    // Resolve and update current id from override chain
    Identifier resolvedId = overrides.get();
    if (resolvedId == null) {
      resolvedId = registry.getDefault().id();
    }
    currentId = resolvedId;

    // Resolve latest current perspective
    Perspective current = registry.getOrDefault(currentId);

    // If cached current is outdated, update the cache
    Perspective cachedCurrent = currentPerspective;
    if (cachedCurrent != current) {
      previousPerspective = cachedCurrent;
      currentPerspective = current;

      cachedCurrent.onDeactivate();
      current.onActivate();

      onCurrentPerspectiveChanged.emit(current);
    }

    // Run perspective client tick
    try {
      current.clientTickWhenActive(minecraft);
    } catch (Throwable e) {
      reportException(current, "clientTick", e);
    }

    if (!current.isAvailable()) {
      wheel.cycleBackward();
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

    Perspective current = currentPerspective;
    Perspective previous = previousPerspective;

    double now = Transition.getTimeMs();

    boolean isTransitioning =
        transition.isInTransition(now)
            && current.allowTransitionIn()
            && (previous == null || previous.allowTransitionOut());

    // Setup context object
    renderTickContext.setup(partialTicks, entity, isTransitioning);

    // Event: render tick
    try {
      currentPerspective.renderTickWhenActive(renderTickContext);
    } catch (Throwable e) {
      reportException(currentPerspective, "renderTick", e);
    }

    // Extract current vanilla state and backup
    Bridge.getCameraPosition(camera, tempPosition);
    Bridge.getCameraRotationQuat(camera, tempRotation);
    backupPosition.set(tempPosition);
    backupRotation.set(tempRotation);

    // 1. Apply Base Perspective
    try {
      currentPerspective.applyTransform(renderTickContext, tempPosition, tempRotation);
    } catch (Throwable e) {
      reportException(currentPerspective, "applyTransform", e);
    }

    // 2. Apply Modifiers (Further mutates the target state BEFORE transition)
    modifiers.applyTransform(renderTickContext, tempPosition, tempRotation);

    // Sanitize and fallback if needed
    boolean posInvalid = !Sanitizer.isFinite(tempPosition);
    boolean rotInvalid = !Sanitizer.isFinite(tempRotation);
    if (posInvalid || rotInvalid) {
      String id = currentPerspective.id().toString();
      throttledAction.run(
          id + ":applyTransform:invalid",
          () ->
              LOGGER.warn(
                  "Perspective '{}' provided invalid state during applyTransform. Falling back to vanilla. pos: {}, rot: {}",
                  id,
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
    Bridge.setCameraRotationQuat(camera, tempRotation);
  }

  /// Called by ModEvents during MODIFY_FIELD_OF_VIEW.
  public float modifyFov(float vanillaFov) {
    float fov = vanillaFov;

    Perspective current = currentPerspective;
    Perspective previous = previousPerspective;

    // Apply Base Perspective
    try {
      fov = current.applyFov(renderTickContext, vanillaFov);
    } catch (Throwable e) {
      reportException(current, "applyFov", e);
      fov = vanillaFov;
    }
    // Sanitize
    boolean fovInvalid = !Sanitizer.isFinite(fov) || fov < 0.0f || fov > 180.0f;
    if (fovInvalid) {
      fov = vanillaFov;
      String id = current.id().toString();
      throttledAction.run(
          id + ":applyFov:invalid",
          () ->
              LOGGER.warn(
                  "Perspective '{}' returned invalid FOV during applyFov. Falling back to vanilla.",
                  id));
    }

    // Apply Modifiers
    fov = modifiers.applyFov(renderTickContext, fov);

    // Apply Transition
    double now = Transition.getTimeMs();
    if (transition.isInTransition(now)
        && current.allowTransitionIn()
        && (previous == null || previous.allowTransitionOut())) {
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

  public void setCurrentId(@NonNull Identifier id) {
    currentId = id;
  }
}
