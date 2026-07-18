package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerspectiveRegistryImpl implements PerspectiveRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveAPI.MOD_NAME);

  public static final PerspectiveRegistryImpl INSTANCE = new PerspectiveRegistryImpl();

  private record Entry(
      @NonNull PerspectiveBehavior behavior,
      @NonNull String id,
      @NonNull Component name,
      @Nullable Component description,
      @NonNull CameraType cameraType,
      boolean switchable,
      int priority,
      @Nullable Identifier icon)
      implements Perspective {

    private static Entry from(@NonNull PerspectiveBehavior behavior) {
      PerspectiveBehavior.Info info =
          behavior.getClass().getAnnotation(PerspectiveBehavior.Info.class);
      if (info == null) {
        throw new ServiceConfigurationError(
            behavior.getClass().getName()
                + " must be annotated with "
                + PerspectiveBehavior.Info.class.getName());
      }
      Identifier icon = info.icon().isEmpty() ? null : Bridge.parseIdentifier(info.icon());

      Component name =
          Component.translatable(
              info.nameKey().isEmpty() ? "perspective." + info.id() + ".name" : info.nameKey());

      Component description =
          info.descriptionKey().isEmpty() ? null : Component.translatable(info.descriptionKey());

      return new Entry(
          behavior,
          info.id(),
          name,
          description,
          info.cameraType(),
          info.switchable(),
          info.priority(),
          icon);
    }

    @Override
    public boolean isAvailable() {
      return behavior.isAvailable();
    }
  }

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();

  private @Nullable String defaultId = null;
  private int defaultPriority = Integer.MIN_VALUE;
  private @Nullable Entry defaultEntry = null;
  private final SimpleEventEmitter.Owned<Void> onUpdate = SimpleEventEmitter.create();

  public PerspectiveRegistryImpl() {}

  public void discoverAndRegister() {
    ServiceLoader<PerspectiveBehavior> loader = ServiceLoader.load(PerspectiveBehavior.class);
    var iterator = loader.iterator();
    while (true) {
      PerspectiveBehavior behavior;
      try {
        if (!iterator.hasNext()) break;
        behavior = iterator.next();
      } catch (ServiceConfigurationError e) {
        LOGGER.warn("Failed to load PerspectiveBehavior implementation", e);
        continue;
      }
      register(behavior);
    }
  }

  public boolean isDefaultFound() {
    return defaultEntry != null;
  }

  public void register(@NonNull PerspectiveBehavior behavior) {
    Entry entry = Entry.from(behavior);
    String id = entry.id();
    LOGGER.info("Registering perspective with id '{}': {}", id, behavior);
    synchronized (this) {
      Entry existing = entries.get(id);

      boolean alreadyRegistered = existing != null && existing.behavior() == behavior;
      if (existing != null) {
        if (existing.behavior() == behavior) {
          LOGGER.warn(
              "Perspective with id '{}' already registered and it has same behavior, ignoring", id);
          return;
        }
        LOGGER.warn("Perspective with id '{}' already registered, replacing", id);
      }
      entries.put(id, entry);

      PerspectiveBehavior.Default defaultAnnotation =
          behavior.getClass().getAnnotation(PerspectiveBehavior.Default.class);
      if (defaultAnnotation != null && defaultAnnotation.priority() >= defaultPriority) {
        defaultId = id;
        defaultPriority = defaultAnnotation.priority();
        defaultEntry = entry;
      }
      onUpdate.emit();
      behavior.init();
    }
  }

  @Override
  public boolean contains(@Nullable String id) {
    if (id == null) return false;
    return entries.containsKey(id);
  }

  @Override
  public @NonNull List<Perspective> getAll() {
    // TODO snapshot
    return new ArrayList<>(entries.values());
  }

  @Override
  public @NonNull SimpleEventEmitter<Void> onUpdate() {
    return onUpdate;
  }

  // region entry

  private @NonNull Entry getEntryOrThrow(@NonNull String id) {
    Entry entry = entries.get(id);
    if (entry == null) {
      throw new IllegalArgumentException("Unregistered perspective id '" + id + "'");
    }
    return entry;
  }

  private @NonNull Entry getDefaultEntry() throws IllegalStateException {
    Entry entry = defaultEntry;
    if (entry == null) {
      throw new IllegalStateException("Default entry is not registered yet");
    }
    return entry;
  }

  private @NonNull Entry getEntryOrDefault(@Nullable String id) {
    if (id != null) {
      Entry entry = entries.get(id);
      if (entry != null) {
        return entry;
      }
    }
    return getDefaultEntry();
  }

  // endregion

  // region perspective

  @Override
  public @Nullable Perspective get(@NonNull String id) {
    return entries.get(id);
  }

  public @NonNull Perspective getDefault() {
    return getDefaultEntry();
  }

  public @NonNull Perspective getOrThrow(@NonNull String id) {
    return getEntryOrThrow(id);
  }

  public @NonNull Perspective getOrDefault(@Nullable String id) {
    return getEntryOrDefault(id);
  }

  // endregion

  // region behavior

  public @NonNull PerspectiveBehavior getDefaultBehavior() {
    return getDefaultEntry().behavior();
  }

  public @NonNull PerspectiveBehavior getBehaviorOrThrow(@NonNull String id) {
    return getEntryOrThrow(id).behavior();
  }

  public @NonNull PerspectiveBehavior getBehaviorOrDefault(@Nullable String id) {
    return getEntryOrDefault(id).behavior();
  }

  // endregion

}
