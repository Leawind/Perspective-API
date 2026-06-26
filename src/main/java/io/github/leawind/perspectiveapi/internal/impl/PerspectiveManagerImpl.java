package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.TransitionController;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveRenderTickContextImpl;
import io.github.leawind.perspectiveapi.internal.logic.builtin.VanillaFirstPersonPerspective;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerspectiveManagerImpl implements PerspectiveManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveManagerImpl.class);
  public static final PerspectiveManagerImpl INSTANCE =
      new PerspectiveManagerImpl(VanillaFirstPersonPerspective.INSTANCE);

  private final @NonNull Identifier defaultId;

  /// Currently used perspective
  private volatile @NonNull Perspective currentPerspective;

  public final SimpleEventEmitter.Owned<Perspective> onCurrentPerspectiveChanged =
      SimpleEventEmitter.create();

  private PerspectiveManagerImpl(@NonNull Perspective defaultPerspective) {
    Objects.requireNonNull(defaultPerspective);
    registry.register(defaultPerspective);
    defaultId = defaultPerspective.id();

    currentPerspective = defaultPerspective;

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

  /// Temporary state extracted from camera for transition start.
  private final PerspectiveState.Mutable cameraState = new PerspectiveStateImpl();

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

    var camera = extractCameraState(cameraState);
    if (camera != null) {
      transition.start(System.currentTimeMillis(), cameraState);
    }
    currentPerspective.onDeactivate();
    perspective.onActivate();

    currentPerspective = perspective;
    onCurrentPerspectiveChanged.emit(perspective);
  }

  // endregion

  // region camera update

  private final PerspectiveRenderTickContextImpl renderTickContext =
      new PerspectiveRenderTickContextImpl(this);

  /// Updates camera position and rotation based on the current perspective.
  ///
  /// @param partialTicks interpolation factor between ticks
  /// @param camera the camera to update
  public void updateCamera(float partialTicks, Camera camera) {
    Perspective perspective = getCurrent();

    long now = System.currentTimeMillis();
    boolean isInTransition = transition.isInTransition(now) && perspective.allowTransition();

    // trigger perspective render tick
    {
      Entity entity = CameraAccessor.of(camera).getEntity();
      if (entity == null) {
        LOGGER.warn("Somehow camera entity is null");
        return;
      }
      renderTickContext.setup(partialTicks, entity, isInTransition);
      perspective.renderTick(renderTickContext);
    }

    PerspectiveState state;
    if (isInTransition) {
      var targetState = perspective.getState();
      if (targetState != null) {
        transition.update(now, targetState);
      }
      state = transition.getCurrentState();
    } else {
      state = perspective.getState();
    }

    if (state != null) {
      if (state.hasPosition()) {
        PerspectiveUtils.setCameraPosition(camera, state.getPosition());
      }
      if (state.hasRotation()) {
        PerspectiveUtils.setCameraRotationQuat(camera, state.getRotation());
      }
    }
  }

  // endregion

  /// Extracts current camera position, rotation, and field of view into the given state.
  ///
  /// @param dest destination state to populate
  /// @return the destination state, or `null` if camera is unavailable
  private static PerspectiveState.@Nullable Mutable extractCameraState(
      PerspectiveState.@NonNull Mutable dest) {
    var camera = Bridge.getMainCamera();
    if (camera == null) return null;

    dest.setHasPosition(true);
    PerspectiveUtils.getCameraPosition(camera, dest.position());

    dest.setHasRotation(true);
    PerspectiveUtils.getCameraRotationQuat(camera, dest.rotation());

    dest.setFieldOfView(Bridge.getFov());

    dest.setFieldOfViewModifier(1.0f);

    return dest;
  }
}
