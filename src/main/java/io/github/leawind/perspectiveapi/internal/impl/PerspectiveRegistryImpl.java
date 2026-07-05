package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import java.util.ArrayList;
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

  private final @NonNull Identifier defaultId;

  // region internal events

  private final List<Runnable> updateListeners = new ArrayList<>();

  // endregion

  public PerspectiveRegistryImpl(@NonNull Perspective defaultPerspective) {
    register(defaultPerspective);
    defaultId = defaultPerspective.id();
  }

  @Override
  public void onUpdate(@NonNull Runnable listener) {
    updateListeners.add(listener);
  }

  @Override
  public @NonNull PerspectiveRegistry register(@NonNull Perspective perspective) {
    var id = perspective.id();
    LOGGER.info("Registering perspective with id '{}': {}", id, perspective);
    synchronized (this) {
      perspectives.put(id, perspective);
      rebuildSnapshot();
    }
    updateListeners.forEach(Runnable::run);
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
  public @NonNull Perspective getDefault() {
    return perspectives.get(defaultId);
  }

  @Override
  public @NonNull Perspective getOrDefault(@Nullable Identifier id) {
    if (id == null) {
      return getDefault();
    }
    return perspectives.getOrDefault(id, getDefault());
  }

  @Override
  public @NonNull List<Perspective> getAll() {
    return allPerspectivesSnapshot;
  }

  private synchronized void rebuildSnapshot() {
    this.allPerspectivesSnapshot = List.copyOf(perspectives.values());
  }
}
