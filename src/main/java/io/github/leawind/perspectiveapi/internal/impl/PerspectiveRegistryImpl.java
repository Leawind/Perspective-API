package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.utils.Exceptions;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerspectiveRegistryImpl implements PerspectiveRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveAPI.MOD_NAME);
  private static final ExtensionInvoker EXTENSIONS = new ExtensionInvoker(LOGGER, "Perspective");

  public static final PerspectiveRegistryImpl INSTANCE = new PerspectiveRegistryImpl();

  private record Entry(
      @NonNull PerspectiveBehavior behavior,
      @NonNull String id,
      @NonNull Component name,
      @Nullable Component description,
      @NonNull BaseType baseType,
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
          info.baseType(),
          info.switchable(),
          info.priority(),
          icon);
    }

    @Override
    public boolean isAvailable() {
      return EXTENSIONS.testOrElse(id, "isAvailable", behavior::isAvailable, false);
    }
  }

  private final Map<String, Entry> entries = new ConcurrentHashMap<>();

  private volatile @Nullable Entry defaultEntry = null;
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
      } catch (Throwable throwable) {
        Exceptions.rethrowIfFatal(throwable);
        LOGGER.error("Failed to load PerspectiveBehavior implementation", throwable);
        continue;
      }
      try {
        registerSilent(behavior);
      } catch (Throwable throwable) {
        Exceptions.rethrowIfFatal(throwable);
        LOGGER.error(
            "Failed to register PerspectiveBehavior implementation {}", behavior, throwable);
      }
    }
    onUpdate.emit();
  }

  public boolean isDefaultFound() {
    return defaultEntry != null;
  }

  public void registerSilent(@NonNull PerspectiveBehavior behavior) {
    Entry entry = Entry.from(behavior);
    String id = entry.id();
    LOGGER.info("Registering perspective with id '{}': {}", id, behavior);
    Entry existing;
    synchronized (this) {
      existing = entries.get(id);

      if (existing != null) {
        if (existing.behavior() == behavior) {
          LOGGER.warn(
              "Perspective with id '{}' already registered and it has same behavior, ignoring", id);
          return;
        }
        LOGGER.warn("Perspective with id '{}' already registered, replacing", id);
      }
      entries.put(id, entry);
      recomputeDefault();
    }

    try {
      behavior.init();
    } catch (Throwable throwable) {
      Exceptions.rethrowIfFatal(throwable);
      synchronized (this) {
        if (existing == null) {
          entries.remove(id, entry);
        } else {
          entries.replace(id, entry, existing);
        }
        recomputeDefault();
      }
      throw Exceptions.propagate(throwable);
    }
  }

  private void recomputeDefault() {
    int bestPriority = Integer.MIN_VALUE;
    @Nullable Entry bestEntry = null;
    for (Entry candidate : entries.values()) {
      PerspectiveBehavior.Default annotation =
          candidate.behavior().getClass().getAnnotation(PerspectiveBehavior.Default.class);
      if (annotation == null) continue;
      int priority = annotation.priority();
      if (bestEntry == null
          || priority > bestPriority
          || (priority == bestPriority && candidate.id().compareTo(bestEntry.id()) < 0)) {
        bestPriority = priority;
        bestEntry = candidate;
      }
    }
    defaultEntry = bestEntry;
  }

  @Override
  public boolean contains(@Nullable String id) {
    if (id == null) return false;
    return entries.containsKey(id);
  }

  @Override
  public @NonNull List<@NonNull Perspective> getAll() {
    return entries.values().stream()
        .sorted(Comparator.comparingInt(Entry::priority).thenComparing(Entry::id))
        .map(entry -> (Perspective) entry)
        .toList();
  }

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

  /// @throws IllegalStateException if default one is not discovered yet
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

  /// @throws IllegalStateException if called before SPI discovery completes during mod loading
  public @NonNull Perspective getDefault() throws IllegalStateException {
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
