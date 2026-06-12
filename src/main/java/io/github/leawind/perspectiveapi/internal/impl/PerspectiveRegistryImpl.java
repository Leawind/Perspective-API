package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.inventory.event.SimpleEventEmitter;
import io.github.leawind.inventory.lock.LockUtils;
import io.github.leawind.perspectiveapi.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerspectiveRegistryImpl implements PerspectiveRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveAPI.MOD_NAME);

  private final ReadWriteLock lock;

  private final Map<Identifier, Perspective> perspectives = new HashMap<>();

  // region internal events

  private final SimpleEventEmitter.Owned<Void> onUpdate = SimpleEventEmitter.create();

  // endregion

  PerspectiveRegistryImpl(ReadWriteLock lock) {
    this.lock = lock;
  }

  public SimpleEventEmitter<Void> onUpdate() {
    return onUpdate;
  }

  @Override
  public @NonNull PerspectiveRegistry register(@NonNull Perspective perspective) {
    var id = perspective.id();

    try (var ignored = LockUtils.writeLock(lock)) {
      LOGGER.info("Registering perspective with id '{}': {}", id, perspective);

      perspectives.put(id, perspective);
      onUpdate.emit();
    }
    return this;
  }

  @Override
  public @Nullable Perspective unregister(@NonNull Identifier id) {
    Perspective removed;
    try (var ignored = LockUtils.writeLock(lock)) {
      removed = perspectives.remove(id);
      onUpdate.emit();
    }
    LOGGER.info("Unregistering perspective with id '{}': {}", id, removed);
    return removed;
  }

  @Override
  public void clear() {
    try (var ignored = LockUtils.writeLock(lock)) {
      perspectives.clear();
      onUpdate.emit();
    }
  }

  @Override
  public boolean contains(Identifier id) {
    try (var ignored = LockUtils.readLock(lock)) {
      return get(id) != null;
    }
  }

  @Override
  public @Nullable Perspective get(Identifier id) {
    if (id == null) {
      return null;
    }
    try (var ignored = LockUtils.readLock(lock)) {
      return perspectives.get(id);
    }
  }

  @Override
  public @NonNull List<Perspective> getAll() {
    try (var ignored = LockUtils.readLock(lock)) {
      return perspectives.values().stream().toList();
    }
  }
}
