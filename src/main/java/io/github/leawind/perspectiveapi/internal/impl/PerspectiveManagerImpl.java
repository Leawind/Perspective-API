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
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerspectiveManagerImpl implements PerspectiveManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveManagerImpl.class);
  public static final PerspectiveManagerImpl INSTANCE =
      new PerspectiveManagerImpl(VanillaFirstPersonPerspective.INSTANCE);

  private final Sanitizer.ThrottledAction throttledAction = new Sanitizer.ThrottledAction(5000);

  private final @NonNull Identifier defaultId;

  /// Previously perspective
  private volatile @NonNull Perspective previousPerspective;

  /// Currently used perspective
  private volatile @NonNull Perspective currentPerspective;

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
    registry.register(defaultPerspective);
    defaultId = defaultPerspective.id();

    previousPerspective = currentPerspective = defaultPerspective;

    overrides.push(PerspectiveCyclerImpl.KEY, Integer.MIN_VALUE, cycler::getActive);

    registry
        .onUpdate()
        .on(
            (perspective) -> {
              if (perspective.id().equals(currentPerspective.id())) {
                setCurrentPerspective(perspective);
              }
            });
  }

  // region components
  private final PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
  private final PerspectiveCyclerImpl cycler = new PerspectiveCyclerImpl();
  private final PerspectiveOverrideChainImpl overrides = new PerspectiveOverrideChainImpl();
  private final TransitionImpl transition = new TransitionImpl();

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

  @Override
  public @NonNull Perspective getDefault() {
    return Objects.requireNonNull(registry().get(defaultId));
  }

  public void resolveAndUpdateCurrentPerspective() {
    setCurrentPerspective(resolveCurrentPerspective());
  }

  private @NonNull Perspective resolveCurrentPerspective() {
    Identifier resolvedId = overrides.resolve(registry::contains);

    if (resolvedId != null) {
      Perspective perspective = registry.get(resolvedId);
      if (perspective != null) {
        return perspective;
      }
    }

    return getDefault();
  }

  private synchronized void setCurrentPerspective(@NonNull Perspective perspective) {
    Objects.requireNonNull(perspective);
    if (perspective == currentPerspective) return;

    previousPerspective = currentPerspective;
    currentPerspective = perspective;

    var camera = Bridge.getMainCamera();
    if (camera == null) return;

    if (!isTempStateInited) {
      Bridge.getCameraPosition(camera, tempPosition);
      Bridge.getCameraRotationQuat(camera, tempRotation);
      tempFov = 70.0f;
      isTempStateInited = true;
    }
    transition.setStartState(System.currentTimeMillis(), tempPosition, tempRotation, tempFov);

    previousPerspective.onDeactivate();
    currentPerspective.onActivate();

    onCurrentPerspectiveChanged.emit(currentPerspective);
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

    long now = System.currentTimeMillis();

    boolean isTransitioning =
        transition.isInTransition(now)
            && currentPerspective.allowTransitionIn()
            && previousPerspective.allowTransitionOut();

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
      transition.updateTransform(now, tempPosition, tempRotation);
      tempPosition.set(transition.getCurrentPosition());
      tempRotation.set(transition.getCurrentRotation());
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

    try {
      fov = currentPerspective.applyFov(renderTickContext, vanillaFov);
    } catch (Throwable e) {
      fovFailed = true;
      reportException(currentPerspective, "applyFov", e);
    }

    boolean fovInvalid = fovFailed || !Sanitizer.isFinite(fov) || fov < 0.0f || fov > 180.0f;
    if (fovInvalid) {
      fov = vanillaFov;
      if (!fovFailed) {
        String id = currentPerspective.id().toString();
        throttledAction.run(
            id + ":applyFov:invalid",
            () ->
                LOGGER.warn(
                    "Perspective '{}' returned invalid FOV during applyFov. Falling back to vanilla.",
                    id));
      }
    }

    long now = System.currentTimeMillis();

    // 计算是否允许过渡

    if (transition.isInTransition(now)
        && currentPerspective.allowTransitionIn()
        && previousPerspective.allowTransitionOut()) {
      tempFov = transition.updateFov(now, fov);
    } else {
      tempFov = fov;
    }
    return tempFov;
  }

  // endregion
}
