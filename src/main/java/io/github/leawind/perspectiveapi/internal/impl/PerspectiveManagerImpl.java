package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.inventory.event.SimpleEventEmitter;
import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.bridge.mixin.CameraAccessor;
import io.github.leawind.perspectiveapi.internal.impl.context.PerspectiveRenderTickContextImpl;
import io.github.leawind.perspectiveapi.internal.logic.VanillaPerspective;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerspectiveManagerImpl implements PerspectiveManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveManagerImpl.class);
  public static final PerspectiveManagerImpl INSTANCE = new PerspectiveManagerImpl();

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl(lock);
  private final PerspectiveCyclerImpl cycler = new PerspectiveCyclerImpl(lock);

  private @Nullable volatile Identifier active;
  private @Nullable volatile Perspective activePerspective;

  // region transition

  private final TransitionImpl transition = new TransitionImpl();

  // endregion

  private PerspectiveManagerImpl() {
    registry.onUpdate().on(() -> setActivePerspective(registry.get(active)));
  }

  // region internal events

  public final SimpleEventEmitter.Owned<@Nullable Perspective> onActivePerspectiveChanged =
      SimpleEventEmitter.create();

  // endregion

  @Override
  public @NonNull PerspectiveRegistry registry() {
    return registry;
  }

  @Override
  public @NonNull PerspectiveCycler cycler() {
    return cycler;
  }

  @Override
  public @NonNull Transition transition() {
    return transition;
  }

  @Override
  public @Nullable Identifier getActiveId() {
    return active;
  }

  @Override
  public void setActive(@Nullable Identifier identifier) {
    active = identifier;
    setActivePerspective(registry.get(active));
  }

  @Override
  public @Nullable Perspective getActivePerspective() {
    return activePerspective;
  }

  private void setActivePerspective(@Nullable Perspective perspective) {
    if (perspective == null) return;
    if (perspective == activePerspective) return;

    transition.start();

    try (var ignored = LockUtils.writeLock(lock)) {
      Perspective ap = activePerspective;
      if (perspective != ap) {
        if (ap != null) {
          ap.onDeactivate();
        }
        perspective.onActivate();

        activePerspective = perspective;
        onActivePerspectiveChanged.emit(perspective);
      }
    }
  }

  private final PerspectiveRenderTickContextImpl renderTickContext =
      new PerspectiveRenderTickContextImpl(this);

  public boolean updateCamera(float partialTicks, Camera camera) {
    Perspective perspective = activePerspective;

    if (perspective instanceof VanillaPerspective) return false;
    if (perspective == null) return false;

    long now = System.currentTimeMillis();
    boolean isInTransition = transition.isInTransition(now) && perspective.allowTransition();

    // Perspective render tick
    {
      Entity entity = ((CameraAccessor) camera).getEntity();
      if (entity == null) {
        LOGGER.warn("Somehow camera entity is null");
        return false;
      }
      renderTickContext.setup(partialTicks, entity, isInTransition);
      perspective.renderTick(renderTickContext);
    }

    if (isInTransition) {
      // Apply transition
      transition.update(now, perspective);
      PerspectiveUtils.setCameraTransform(
          camera, transition.getPosition(), transition.getRotation());
    } else {
      // Apply perspective to camera
      PerspectiveUtils.setCameraTransform(
          camera, perspective.getPosition(), perspective.getRotation());
    }

    return true;
  }
}
