package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveRenderTickContextImpl;
import io.github.leawind.perspectiveapi.internal.logic.builtin.VanillaFirstPersonPerspective;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;
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
  public @NonNull Transition transition() {
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

    transition.start(System.currentTimeMillis());
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

    Vector3dc position;
    Quaternionfc rotation;
    if (isInTransition) {
      transition.update(now, perspective);
      position = transition.getPosition();
      rotation = transition.getRotation();
    } else {
      position = perspective.getPosition();
      rotation = perspective.getRotation();
    }

    // Apply to camera
    if (position != null) PerspectiveUtils.setCameraPosition(camera, position);
    if (rotation != null) PerspectiveUtils.setCameraRotationQuat(camera, rotation);
  }

  // endregion
}
