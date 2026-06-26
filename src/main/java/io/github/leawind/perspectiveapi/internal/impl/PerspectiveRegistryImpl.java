package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.inventory.event.SimpleEventEmitter;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerspectiveRegistryImpl implements PerspectiveRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveAPI.MOD_NAME);

  private final Map<Identifier, Perspective> perspectives = new ConcurrentHashMap<>();

  private volatile List<Perspective> allPerspectivesSnapshot = List.of();

  // region internal events

  private final SimpleEventEmitter.Owned<Perspective> onUpdate = SimpleEventEmitter.create();

  // endregion

  PerspectiveRegistryImpl() {}

  /// Emitted when the registry is updated (perspective added).
  public SimpleEventEmitter<Perspective> onUpdate() {
    return onUpdate;
  }

  @Override
  public @NonNull PerspectiveRegistry register(@NonNull Perspective perspective) {
    var id = perspective.id();

    LOGGER.info("Registering perspective with id '{}': {}", id, perspective);
    synchronized (this) {
      perspectives.put(id, perspective);
      rebuildSnapshot();
    }
    onUpdate.emit(perspective);
    return this;
  }

  @Override
  public boolean contains(@Nullable Identifier id) {
    return get(id) != null;
  }

  @Override
  public @Nullable Perspective get(@Nullable Identifier id) {
    if (id == null) {
      return null;
    }
    return perspectives.get(id);
  }

  @Override
  public @NonNull List<Perspective> getAll() {
    return allPerspectivesSnapshot;
  }

  private synchronized void rebuildSnapshot() {
    this.allPerspectivesSnapshot = List.copyOf(perspectives.values());
  }
}
