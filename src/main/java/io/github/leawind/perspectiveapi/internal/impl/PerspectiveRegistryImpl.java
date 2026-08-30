package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistration;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.utils.Exceptions;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
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

  private static final class RegisteredPerspective implements Perspective {
    private final PerspectiveBehavior behavior;
    private final @Nullable Integer defaultPriority;
    private final PerspectiveInfo info;
    private volatile boolean initialized;

    private RegisteredPerspective(
        @NonNull PerspectiveInfo info,
        @Nullable Integer defaultPriority,
        @NonNull PerspectiveBehavior behavior) {
      this.behavior = behavior;
      this.defaultPriority = defaultPriority;
      this.info = info;
    }

    private static @NonNull RegisteredPerspective fromDeclaration(
        @NonNull PerspectiveBehavior behavior) {
      PerspectiveInfo.Declaration declaration = getDeclaration(behavior);

      PerspectiveInfo.Default defaultAnnotation =
          behavior.getClass().getAnnotation(PerspectiveInfo.Default.class);
      Integer defaultPriority = defaultAnnotation == null ? null : defaultAnnotation.priority();
      return new RegisteredPerspective(createInfo(declaration), defaultPriority, behavior);
    }

    private static @NonNull PerspectiveInfo createInfo(
        PerspectiveInfo.@NonNull Declaration declaration) {
      String id = declaration.id();
      Component name =
          Component.translatable(
              declaration.nameKey().isEmpty()
                  ? "perspective." + id + ".name"
                  : declaration.nameKey());
      Component description =
          declaration.descriptionKey().isEmpty()
              ? null
              : Component.translatable(declaration.descriptionKey());
      Identifier icon =
          declaration.icon().isEmpty() ? null : Bridge.parseIdentifier(declaration.icon());
      return new PerspectiveInfo(
          id,
          name,
          description,
          declaration.order(),
          icon,
          Set.copyOf(Arrays.asList(declaration.traits())));
    }

    private static PerspectiveInfo.@NonNull Declaration getDeclaration(
        @NonNull PerspectiveBehavior behavior) {
      PerspectiveInfo.Declaration declaration =
          behavior.getClass().getAnnotation(PerspectiveInfo.Declaration.class);
      if (declaration == null) {
        throw new ServiceConfigurationError(
            behavior.getClass().getName()
                + " must be annotated with "
                + PerspectiveInfo.Declaration.class.getName());
      }
      if (declaration.id().isEmpty()) {
        throw new ServiceConfigurationError(
            behavior.getClass().getName() + " must declare a non-empty perspective ID");
      }
      return declaration;
    }

    private boolean evaluateAvailability() {
      return EXTENSIONS.testOrElse(info.id(), "isAvailable", behavior::isAvailable, false);
    }

    @Override
    public @NonNull PerspectiveInfo info() {
      return info;
    }

    @Override
    public boolean isAvailable() {
      return evaluateAvailability();
    }
  }

  private static final class Registration implements PerspectiveRegistration {
    private final PerspectiveRegistryImpl owner;
    private final RegisteredPerspective entry;

    private Registration(
        @NonNull PerspectiveRegistryImpl owner, @NonNull RegisteredPerspective entry) {
      this.owner = owner;
      this.entry = entry;
    }

    @Override
    public @NonNull Perspective perspective() {
      return entry;
    }

    @Override
    public boolean unregister() {
      return owner.unregister(entry);
    }
  }

  private final Map<String, RegisteredPerspective> entries = new ConcurrentHashMap<>();
  private final IdentityHashMap<PerspectiveBehavior, RegisteredPerspective> entriesByBehavior =
      new IdentityHashMap<>();

  private volatile @Nullable RegisteredPerspective defaultEntry;
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

  /// Registers a service-discovered behavior without emitting an update event.
  public void registerSilent(@NonNull PerspectiveBehavior behavior) {
    Objects.requireNonNull(behavior);
    RegisteredPerspective entry = RegisteredPerspective.fromDeclaration(behavior);
    String id = entry.info.id();
    RegisteredPerspective displaced;
    synchronized (this) {
      rejectDuplicateBehavior(behavior);
      displaced = entries.get(id);
      if (displaced != null && compareRegistration(entry, displaced) >= 0) {
        LOGGER.warn(
            "Perspective with id '{}' is already registered by {}. Ignoring lower-precedence "
                + "candidate {}",
            id,
            displaced.behavior.getClass().getName(),
            behavior.getClass().getName());
        return;
      }

      if (displaced == null) {
        LOGGER.info("Registering perspective with id '{}': {}", id, behavior);
      } else {
        LOGGER.warn(
            "Perspective with id '{}' is already registered by {}. Replacing it with {}",
            id,
            displaced.behavior.getClass().getName(),
            behavior.getClass().getName());
        entriesByBehavior.remove(displaced.behavior);
      }
      entries.put(id, entry);
      entriesByBehavior.put(behavior, entry);
    }

    initializeOrRollback(entry, displaced);
  }

  @Override
  public @NonNull PerspectiveRegistration register(
      @NonNull PerspectiveInfo info, @NonNull PerspectiveBehavior behavior) {
    return registerRuntime(info, null, behavior);
  }

  @Override
  public @NonNull PerspectiveRegistration registerDefault(
      @NonNull PerspectiveInfo info, int defaultPriority, @NonNull PerspectiveBehavior behavior) {
    return registerRuntime(info, defaultPriority, behavior);
  }

  private @NonNull PerspectiveRegistration registerRuntime(
      @NonNull PerspectiveInfo info,
      @Nullable Integer defaultPriority,
      @NonNull PerspectiveBehavior behavior) {
    Objects.requireNonNull(info);
    Objects.requireNonNull(behavior);
    RegisteredPerspective entry = new RegisteredPerspective(info, defaultPriority, behavior);
    String id = info.id();
    synchronized (this) {
      rejectDuplicateBehavior(behavior);
      if (entries.containsKey(id)) {
        throw new IllegalArgumentException("Perspective id is already registered: '" + id + "'");
      }
      entries.put(id, entry);
      entriesByBehavior.put(behavior, entry);
    }

    initializeOrRollback(entry, null);
    onUpdate.emit();
    return new Registration(this, entry);
  }

  private void rejectDuplicateBehavior(@NonNull PerspectiveBehavior behavior) {
    RegisteredPerspective existing = entriesByBehavior.get(behavior);
    if (existing != null) {
      throw new IllegalArgumentException(
          "Perspective behavior instance is already registered as '" + existing.info.id() + "'");
    }
  }

  private void initializeOrRollback(
      @NonNull RegisteredPerspective entry, @Nullable RegisteredPerspective displaced) {
    try {
      entry.behavior.initialize();
      synchronized (this) {
        entry.initialized = true;
        recomputeDefault();
      }
    } catch (Throwable throwable) {
      Exceptions.rethrowIfFatal(throwable);
      synchronized (this) {
        entries.remove(entry.info.id(), entry);
        entriesByBehavior.remove(entry.behavior);
        if (displaced != null) {
          entries.put(displaced.info.id(), displaced);
          entriesByBehavior.put(displaced.behavior, displaced);
        }
        recomputeDefault();
      }
      throw Exceptions.propagate(throwable);
    }
  }

  /// Orders duplicate service registrations by their documented precedence.
  private static int compareRegistration(
      @NonNull RegisteredPerspective left, @NonNull RegisteredPerspective right) {
    int order = Integer.compare(left.info.order(), right.info.order());
    if (order != 0) return order;
    return left.behavior.getClass().getName().compareTo(right.behavior.getClass().getName());
  }

  private void recomputeDefault() {
    int bestPriority = Integer.MIN_VALUE;
    @Nullable RegisteredPerspective bestEntry = null;
    for (RegisteredPerspective candidate : entries.values()) {
      if (!candidate.initialized) continue;
      Integer priority = candidate.defaultPriority;
      if (priority == null) continue;
      if (bestEntry == null
          || priority > bestPriority
          || (priority == bestPriority && candidate.info.id().compareTo(bestEntry.info.id()) < 0)) {
        bestPriority = priority;
        bestEntry = candidate;
      }
    }
    defaultEntry = bestEntry;
  }

  private boolean isRegistered(@NonNull RegisteredPerspective entry) {
    return entries.get(entry.info.id()) == entry;
  }

  private boolean unregister(@NonNull RegisteredPerspective entry) {
    synchronized (this) {
      if (!isRegistered(entry)) return false;
      if (entry.defaultPriority != null && hasNoOtherDefault(entry)) {
        throw new IllegalStateException(
            "Cannot unregister the last default perspective: '" + entry.info.id() + "'");
      }
      entries.remove(entry.info.id(), entry);
      entriesByBehavior.remove(entry.behavior);
      recomputeDefault();
    }
    onUpdate.emit();
    return true;
  }

  private boolean hasNoOtherDefault(@NonNull RegisteredPerspective removed) {
    for (RegisteredPerspective candidate : entries.values()) {
      if (candidate != removed && candidate.initialized && candidate.defaultPriority != null) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean contains(@Nullable String id) {
    return id != null && entries.containsKey(id);
  }

  /// Returns an unmodifiable snapshot of all registered perspectives.
  ///
  /// The list is sorted by ascending priority and then by ID. It includes perspectives that are
  /// currently unavailable.
  public @NonNull List<@NonNull Perspective> getAllPerspectives() {
    return entries.values().stream()
        .sorted(
            Comparator.comparingInt(
                    (RegisteredPerspective perspective) -> perspective.info.order())
                .thenComparing(perspective -> perspective.info.id()))
        .map(entry -> (Perspective) entry)
        .toList();
  }

  public @NonNull SimpleEventEmitter<Void> onUpdate() {
    return onUpdate;
  }

  private @NonNull RegisteredPerspective getEntryOrThrow(@NonNull String id) {
    RegisteredPerspective entry = entries.get(id);
    if (entry == null) {
      throw new IllegalArgumentException("Unregistered perspective id '" + id + "'");
    }
    return entry;
  }

  private @NonNull RegisteredPerspective getDefaultEntry() throws IllegalStateException {
    RegisteredPerspective entry = defaultEntry;
    if (entry == null) {
      throw new IllegalStateException("Default entry is not registered yet");
    }
    return entry;
  }

  private @NonNull RegisteredPerspective getEntryOrDefault(@Nullable String id) {
    if (id != null) {
      RegisteredPerspective entry = entries.get(id);
      if (entry != null) return entry;
    }
    return getDefaultEntry();
  }

  @Override
  public @Nullable Perspective get(@NonNull String id) {
    return entries.get(Objects.requireNonNull(id));
  }

  public @NonNull Perspective getDefault() throws IllegalStateException {
    return getDefaultEntry();
  }

  public @NonNull Perspective getOrThrow(@NonNull String id) {
    return getEntryOrThrow(id);
  }

  public @NonNull Perspective getOrDefault(@Nullable String id) {
    return getEntryOrDefault(id);
  }

  public @NonNull PerspectiveBehavior getDefaultBehavior() {
    return getDefaultEntry().behavior;
  }

  public @NonNull PerspectiveBehavior getBehaviorOrThrow(@NonNull String id) {
    return getEntryOrThrow(id).behavior;
  }

  public @NonNull PerspectiveBehavior getBehaviorOrDefault(@Nullable String id) {
    return getEntryOrDefault(id).behavior;
  }
}
