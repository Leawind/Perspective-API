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
import io.github.leawind.perspectiveapi.internal.logic.vanilla.VanillaFirstPersonPerspective;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerspectiveManagerImpl implements PerspectiveManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveManagerImpl.class);
  public static final PerspectiveManagerImpl INSTANCE =
      new PerspectiveManagerImpl(VanillaFirstPersonPerspective.INSTANCE);

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

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

    pushOverride(PerspectiveCyclerImpl.KEY, Integer.MIN_VALUE, cycler::getActive);
  }

  // region components
  private final PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl(lock);
  private final PerspectiveCyclerImpl cycler = new PerspectiveCyclerImpl(lock);
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
  public @NonNull Transition transition() {
    return transition;
  }

  // endregion

  // region override chain

  /// Override chain sorted by priority descending (highest priority first).
  private final List<OverrideEntry> overrideChain = new ArrayList<>();

  @Override
  public void pushOverride(
      @NonNull Identifier key, int priority, @NonNull Supplier<@Nullable Identifier> supplier) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(supplier);

    try (var ignored = LockUtils.writeLock(lock)) {
      overrideChain.removeIf(e -> e.key().equals(key));

      var entry = new OverrideEntry(key, priority, supplier);
      int insertIdx = 0;
      for (int i = 0; i < overrideChain.size(); i++) {
        if (overrideChain.get(i).priority() >= priority) {
          insertIdx = i + 1;
        } else {
          break;
        }
      }
      overrideChain.add(insertIdx, entry);
    }
  }

  @Override
  public void popOverride(@NonNull Identifier key) {
    Objects.requireNonNull(key);

    try (var ignored = LockUtils.writeLock(lock)) {
      overrideChain.removeIf(e -> e.key().equals(key));
    }
  }

  @Override
  public boolean hasOverride(@NonNull Identifier key) {
    Objects.requireNonNull(key);

    try (var ignored = LockUtils.readLock(lock)) {
      return overrideChain.stream().anyMatch(e -> e.key().equals(key));
    }
  }

  // endregion

  // region perspective management

  @Override
  public @NonNull Perspective getCurrentPerspective() {
    return currentPerspective;
  }

  @Override
  public @NonNull Identifier getDefault() {
    return defaultId;
  }

  /// Removes all override entries except the cycler node.
  public void clearOverridesExceptCycler() {
    try (var ignored = LockUtils.writeLock(lock)) {
      overrideChain.removeIf(e -> !e.key().equals(PerspectiveCyclerImpl.KEY));
    }
  }

  public void resolveAndUpdatePerspective() {
    setCurrentPerspective(resolveCurrentPerspective());
  }

  private @NonNull Perspective resolveCurrentPerspective() {
    try (var ignored = LockUtils.readLock(lock)) {
      for (OverrideEntry entry : overrideChain) {
        Identifier id = entry.supplier().get();
        if (id == null) continue;

        Perspective perspective = registry.get(id);
        if (perspective == null) continue;
        if (!perspective.isAvailable()) continue;

        return perspective;
      }
    }
    return getDefaultPerspective();
  }

  private void setCurrentPerspective(@NonNull Perspective perspective) {
    Objects.requireNonNull(perspective);

    try (var ignored = LockUtils.writeLock(lock)) {
      if (perspective == currentPerspective) return;

      transition.start(System.currentTimeMillis());
      currentPerspective.onDeactivate();
      perspective.onActivate();

      currentPerspective = perspective;
      onCurrentPerspectiveChanged.emit(perspective);
    }
  }

  // endregion

  // region camera update

  private final PerspectiveRenderTickContextImpl renderTickContext =
      new PerspectiveRenderTickContextImpl(this);

  public void updateCamera(float partialTicks, Camera camera) {
    Perspective perspective = getCurrentPerspective();

    if (!perspective.shouldOverrideVanillaCamera()) return;

    long now = System.currentTimeMillis();
    boolean isInTransition = transition.isInTransition(now) && perspective.allowTransition();

    // trigger perspective render tick
    {
      Entity entity = ((CameraAccessor) camera).getEntity();
      if (entity == null) {
        LOGGER.warn("Somehow camera entity is null");
        return;
      }
      renderTickContext.setup(partialTicks, entity, isInTransition);
      perspective.renderTick(renderTickContext);
    }

    // modify camera position and rotation
    if (isInTransition) {
      transition.update(now, perspective);
      PerspectiveUtils.setCameraTransform(
          camera, transition.getPosition(), transition.getRotation());
    } else {
      PerspectiveUtils.setCameraTransform(
          camera, perspective.getPosition(), perspective.getRotation());
    }
  }

  // endregion
}
