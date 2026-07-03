package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.TransitionController;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveContextImpl;
import io.github.leawind.perspectiveapi.internal.logic.builtin.VanillaFirstPersonPerspective;
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

public final class PerspectiveManagerImpl implements PerspectiveManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveManagerImpl.class);
  public static final PerspectiveManagerImpl INSTANCE =
      new PerspectiveManagerImpl(VanillaFirstPersonPerspective.INSTANCE);

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

  public void reportException(Perspective perspective, String phase, Throwable throwable) {
    String id = perspective.id().toString();
    throttledAction.run(
        id + ":" + phase + ":exception",
        () -> LOGGER.warn("Perspective '{}' threw an exception during {}.", id, phase, throwable));
  }

  private PerspectiveManagerImpl(@NonNull Perspective defaultPerspective) {
    Objects.requireNonNull(defaultPerspective);
    registry = new PerspectiveRegistryImpl(defaultPerspective);
    currentId = defaultPerspective.id();
    currentPerspective = defaultPerspective;

    overrides.push(PerspectiveCyclerImpl.KEY, Integer.MIN_VALUE, cycler::getActive);

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
  private final PerspectiveCyclerImpl cycler = new PerspectiveCyclerImpl();
  private final PerspectiveOverrideChainImpl overrides = new PerspectiveOverrideChainImpl();
  private final Transition transition = new TransitionImpl();

  @Override
  public @NonNull PerspectiveRegistry registry() {
    return registry;
  }

  @Override
  public @NonNull PerspectiveCycler cycler() {
    return cycler;
  }

  @Override
  public @NonNull PerspectiveOverrideChain overrides() {
    return overrides;
  }

  @Override
  public @NonNull TransitionController transition() {
    return transition;
  }

  // endregion

  // region perspective management

  @Override
  public @NonNull Perspective getCurrent() {
    return currentPerspective;
  }

  public void clientTick(Minecraft minecraft) {
    // Resolve and update current id from override chain
    Identifier resolvedId = overrides.resolve(registry::contains);
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
      current.clientTick(minecraft);
    } catch (Throwable e) {
      reportException(current, "clientTick", e);
    }

    if (!current.isAvailable()) {
      cycler().switchToPreviousAvailable(registry());
    }
  }

  // endregion

  // region camera update

  private final PerspectiveContextImpl renderTickContext = new PerspectiveContextImpl(this);
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
      currentPerspective.renderTick(renderTickContext);
    } catch (Throwable e) {
      reportException(currentPerspective, "renderTick", e);
    }

    // Extract current vanilla state and backup
    Bridge.getCameraPosition(camera, tempPosition);
    Bridge.getCameraRotationQuat(camera, tempRotation);
    backupPosition.set(tempPosition);
    backupRotation.set(tempRotation);

    // Apply transform
    try {
      currentPerspective.applyTransform(renderTickContext, tempPosition, tempRotation);
    } catch (Throwable e) {
      reportException(currentPerspective, "applyTransform", e);
    }

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
    }
    isTempStateInited = true;

    // Commit to camera
    Bridge.setCameraPosition(camera, tempPosition);
    Bridge.setCameraRotationQuat(camera, tempRotation);
  }

  /// Called by ModEvents during MODIFY_FIELD_OF_VIEW.
  public float modifyFov(float vanillaFov) {
    float fov = vanillaFov;
    boolean fovFailed = false;

    Perspective current = currentPerspective;
    Perspective previous = previousPerspective;

    try {
      fov = current.applyFov(renderTickContext, vanillaFov);
    } catch (Throwable e) {
      fovFailed = true;
      reportException(current, "applyFov", e);
    }

    boolean fovInvalid = fovFailed || !Sanitizer.isFinite(fov) || fov < 0.0f || fov > 180.0f;
    if (fovInvalid) {
      fov = vanillaFov;
      if (!fovFailed) {
        String id = current.id().toString();
        throttledAction.run(
            id + ":applyFov:invalid",
            () ->
                LOGGER.warn(
                    "Perspective '{}' returned invalid FOV during applyFov. Falling back to vanilla.",
                    id));
      }
    }

    double now = Transition.getTimeMs();
    if (transition.isInTransition(now)
        && current.allowTransitionIn()
        && (previous == null || previous.allowTransitionOut())) {
      tempFov = transition.updateFov(now, fov);
    } else {
      tempFov = fov;
    }
    return tempFov;
  }

  // endregion
}
