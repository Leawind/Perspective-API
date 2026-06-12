package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.inventory.event.SimpleEventEmitter;
import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PerspectiveManagerImpl implements PerspectiveManager {
  public static final PerspectiveManagerImpl INSTANCE = new PerspectiveManagerImpl();

  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl(lock);
  private final PerspectiveCyclerImpl cycler = new PerspectiveCyclerImpl(lock);

  private @Nullable volatile Identifier active;
  private @Nullable volatile Perspective activePerspective;

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
    if (perspective == null) {
      return;
    }

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
}
