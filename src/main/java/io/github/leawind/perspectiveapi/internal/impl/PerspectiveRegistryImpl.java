package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveMeta;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
      @NonNull Perspective perspective,
      @NonNull String id,
      @Nullable Component name,
      @Nullable Component description,
      @NonNull CameraType cameraType,
      boolean switchable,
      int priority,
      @Nullable Identifier icon)
      implements PerspectiveMeta {

    private static Entry from(@NonNull Perspective perspective) {
      Perspective.Meta meta = perspective.getClass().getAnnotation(Perspective.Meta.class);
      if (meta == null) {
        throw new IllegalArgumentException(
            perspective.getClass().getName() + " must be annotated with @Perspective.Meta");
      }
      Identifier icon = meta.icon().isEmpty() ? null : Bridge.parseIdentifier(meta.icon());

      Component name =
          Component.translatable(
              meta.nameKey().isEmpty() ? "perspective." + meta.id() + ".name" : meta.nameKey());

      Component description =
          meta.descriptionKey().isEmpty() ? null : Component.translatable(meta.descriptionKey());

      return new Entry(
          perspective,
          meta.id(),
          name,
          description,
          meta.cameraType(),
          meta.switchable(),
          meta.priority(),
          icon);
    }
  }

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();

  private @Nullable String defaultId = null;
  private int defaultPriority = Integer.MIN_VALUE;
  private @Nullable Entry defaultEntry = null;
  private final SimpleEventEmitter.Owned<Void> onUpdate = SimpleEventEmitter.create();

  public PerspectiveRegistryImpl() {}

  public void discoverAndRegister() {
    ServiceLoader<Perspective> loader = ServiceLoader.load(Perspective.class);
    var iterator = loader.iterator();
    while (true) {
      Perspective perspective;
      try {
        if (!iterator.hasNext()) break;
        perspective = iterator.next();
      } catch (ServiceConfigurationError e) {
        LOGGER.warn("Failed to load Perspective implementation", e);
        continue;
      }
      register(perspective);
    }
  }

  public boolean isDefaultFound() {
    return defaultEntry != null;
  }

  public void register(@NonNull Perspective perspective) {
    Entry entry = Entry.from(perspective);
    String id = entry.id();
    LOGGER.info("Registering perspective with id '{}': {}", id, perspective);
    synchronized (this) {
      if (entries.containsKey(id)) {
        LOGGER.warn("Perspective with id '{}' already registered, replacing", id);
      }
      entries.put(id, entry);

      Perspective.Default defaultAnnotation =
          perspective.getClass().getAnnotation(Perspective.Default.class);
      if (defaultAnnotation != null && defaultAnnotation.priority() >= defaultPriority) {
        defaultId = id;
        defaultPriority = defaultAnnotation.priority();
        defaultEntry = entry;
      }
      onUpdate.emit();
    }
  }

  // region id

  public @NonNull String getDefaultId() {
    return Objects.requireNonNull(defaultId, "No default perspective found");
  }

  @Override
  public boolean contains(@Nullable String id) {
    if (id == null) return false;
    return entries.containsKey(id);
  }

  @SuppressWarnings("unchecked")
  @Override
  public @NonNull List<PerspectiveMeta> getAll() {
    // TODO snapshot
    return (List<PerspectiveMeta>) (List<?>) entries.values().stream().toList();
  }

  @Override
  public @NonNull SimpleEventEmitter<Void> onUpdate() {
    return onUpdate;
  }

  // endregion

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

  public @NonNull Perspective getDefaultPerspective() {
    return getDefaultEntry().perspective();
  }

  public @NonNull Perspective getPerspectiveOrThrow(@NonNull String id) {
    return getEntryOrThrow(id).perspective();
  }

  public @NonNull Perspective getPerspectiveOrDefault(@Nullable String id) {
    return getEntryOrDefault(id).perspective();
  }

  // endregion

  // region meta

  public @NonNull PerspectiveMeta getDefaultMeta() {
    return getDefaultEntry();
  }

  public @NonNull PerspectiveMeta getMetaOrThrow(@NonNull String id) {
    return getEntryOrThrow(id);
  }

  public @NonNull PerspectiveMeta getMetaOrDefault(@Nullable String id) {
    return getEntryOrDefault(id);
  }

  // endregion
}
