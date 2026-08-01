package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistration;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.PerspectiveTraitRegistration;
import io.github.leawind.perspectiveapi.internal.utils.Exceptions;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerspectiveRegistryImpl implements PerspectiveRegistry {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveAPI.MOD_NAME);
  private static final ExtensionInvoker EXTENSIONS = new ExtensionInvoker(LOGGER, "Perspective");

  public static final PerspectiveRegistryImpl INSTANCE = new PerspectiveRegistryImpl();

  private static final class RegisteredPerspective implements Perspective {
    private final PerspectiveRegistryImpl owner;
    private final PerspectiveBehavior behavior;
    private final @Nullable Integer defaultPriority;
    private volatile PerspectiveInfo info;
    private volatile Set<String> effectiveTraits;
    private volatile boolean initialized;

    private RegisteredPerspective(
        @NonNull PerspectiveRegistryImpl owner,
        @NonNull PerspectiveInfo info,
        @Nullable Integer defaultPriority,
        @NonNull PerspectiveBehavior behavior) {
      this.owner = owner;
      this.behavior = behavior;
      this.defaultPriority = defaultPriority;
      this.info = info;
      effectiveTraits = info.traits();
    }

    private static @NonNull RegisteredPerspective fromDeclaration(
        @NonNull PerspectiveRegistryImpl owner, @NonNull PerspectiveBehavior behavior) {
      PerspectiveInfo.Declaration declaration = getDeclaration(behavior);

      PerspectiveInfo.Default defaultAnnotation =
          behavior.getClass().getAnnotation(PerspectiveInfo.Default.class);
      Integer defaultPriority = defaultAnnotation == null ? null : defaultAnnotation.priority();
      return new RegisteredPerspective(
          owner, PerspectiveInfo.fromDeclaration(declaration), defaultPriority, behavior);
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
    public @NonNull Set<@NonNull String> traits() {
      return effectiveTraits;
    }

    @Override
    public boolean isAvailable() {
      return owner.availabilitySnapshot.isAvailable(this);
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
    public boolean isRegistered() {
      return owner.isRegistered(entry);
    }

    @Override
    public void updateInfo(@NonNull PerspectiveInfo info) {
      owner.updateInfo(entry, info);
    }

    @Override
    public boolean unregister() {
      return owner.unregister(entry);
    }
  }

  private static final class TraitContribution implements PerspectiveTraitRegistration {
    private final PerspectiveRegistryImpl owner;
    private final String contributorId;
    private final String perspectiveId;
    private final Set<String> traits;

    private TraitContribution(
        @NonNull PerspectiveRegistryImpl owner,
        @NonNull String contributorId,
        @NonNull String perspectiveId,
        @NonNull Set<String> traits) {
      this.owner = owner;
      this.contributorId = contributorId;
      this.perspectiveId = perspectiveId;
      this.traits = traits;
    }

    @Override
    public boolean isRegistered() {
      return owner.isRegistered(this);
    }

    @Override
    public boolean unregister() {
      return owner.unregister(this);
    }
  }

  private static final class AvailabilitySnapshot {
    private final Map<RegisteredPerspective, Boolean> values = new ConcurrentHashMap<>();

    private boolean isAvailable(@NonNull RegisteredPerspective entry) {
      return values.computeIfAbsent(entry, RegisteredPerspective::evaluateAvailability);
    }
  }

  private final Map<String, RegisteredPerspective> entries = new ConcurrentHashMap<>();
  private final IdentityHashMap<PerspectiveBehavior, RegisteredPerspective> entriesByBehavior =
      new IdentityHashMap<>();
  private final Map<String, List<TraitContribution>> traitContributions = new HashMap<>();

  private volatile @Nullable RegisteredPerspective defaultEntry;
  private volatile AvailabilitySnapshot availabilitySnapshot = new AvailabilitySnapshot();
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

  /// Starts a new lazily evaluated perspective-availability snapshot.
  ///
  /// Within one snapshot, each registered perspective behavior is evaluated at most once. All
  /// subsequent {@link Perspective#isAvailable()} calls reuse that result until this method is
  /// called for the next client tick.
  public void beginAvailabilitySnapshot() {
    availabilitySnapshot = new AvailabilitySnapshot();
  }

  /// Registers a service-discovered behavior without emitting an update event.
  public void registerSilent(@NonNull PerspectiveBehavior behavior) {
    Objects.requireNonNull(behavior);
    RegisteredPerspective entry = RegisteredPerspective.fromDeclaration(this, behavior);
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
      refreshEffectiveTraits(entry);
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
    RegisteredPerspective entry = new RegisteredPerspective(this, info, defaultPriority, behavior);
    String id = info.id();
    synchronized (this) {
      rejectDuplicateBehavior(behavior);
      if (entries.containsKey(id)) {
        throw new IllegalArgumentException("Perspective id is already registered: '" + id + "'");
      }
      entries.put(id, entry);
      entriesByBehavior.put(behavior, entry);
      refreshEffectiveTraits(entry);
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
      entry.behavior.init();
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
    int priority = Integer.compare(left.info.priority(), right.info.priority());
    if (priority != 0) return priority;
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

  private synchronized boolean isRegistered(@NonNull TraitContribution contribution) {
    List<TraitContribution> contributions = traitContributions.get(contribution.perspectiveId);
    return contributions != null && contributions.contains(contribution);
  }

  private void updateInfo(@NonNull RegisteredPerspective entry, @NonNull PerspectiveInfo info) {
    Objects.requireNonNull(info);
    synchronized (this) {
      String id = entry.info.id();
      if (!id.equals(info.id())) {
        throw new IllegalArgumentException("A perspective registration cannot change its ID");
      }
      if (!isRegistered(entry)) {
        throw new IllegalStateException(
            "Perspective registration is no longer present: '" + id + "'");
      }
      if (entry.info.equals(info)) return;
      entry.info = info;
      refreshEffectiveTraits(entry);
    }
    onUpdate.emit();
  }

  @Override
  public @NonNull PerspectiveTraitRegistration contributeTraits(
      @NonNull String contributorId,
      @NonNull String perspectiveId,
      @NonNull Collection<@NonNull String> traits) {
    Objects.requireNonNull(contributorId);
    Objects.requireNonNull(perspectiveId);
    Objects.requireNonNull(traits);
    if (contributorId.isEmpty()) {
      throw new IllegalArgumentException("Contributor id must not be empty");
    }
    if (perspectiveId.isEmpty()) {
      throw new IllegalArgumentException("Perspective id must not be empty");
    }

    Set<String> copiedTraits = new HashSet<>();
    for (String trait : traits) {
      PerspectiveInfo.validateTrait(trait);
      copiedTraits.add(trait);
    }
    if (copiedTraits.isEmpty()) {
      throw new IllegalArgumentException("Trait contribution must not be empty");
    }

    TraitContribution contribution =
        new TraitContribution(this, contributorId, perspectiveId, Set.copyOf(copiedTraits));
    synchronized (this) {
      traitContributions
          .computeIfAbsent(perspectiveId, ignored -> new ArrayList<>())
          .add(contribution);
      RegisteredPerspective entry = entries.get(perspectiveId);
      if (entry != null) refreshEffectiveTraits(entry);
    }
    LOGGER.info(
        "Registering trait contribution from '{}' for '{}': {}",
        contributorId,
        perspectiveId,
        contribution.traits);
    onUpdate.emit();
    return contribution;
  }

  private boolean unregister(@NonNull TraitContribution contribution) {
    synchronized (this) {
      List<TraitContribution> contributions = traitContributions.get(contribution.perspectiveId);
      if (contributions == null || !contributions.remove(contribution)) return false;
      if (contributions.isEmpty()) traitContributions.remove(contribution.perspectiveId);
      RegisteredPerspective entry = entries.get(contribution.perspectiveId);
      if (entry != null) refreshEffectiveTraits(entry);
    }
    LOGGER.info(
        "Removing trait contribution from '{}' for '{}': {}",
        contribution.contributorId,
        contribution.perspectiveId,
        contribution.traits);
    onUpdate.emit();
    return true;
  }

  private void refreshEffectiveTraits(@NonNull RegisteredPerspective entry) {
    Set<String> effectiveTraits = new HashSet<>(entry.info.traits());
    List<TraitContribution> contributions = traitContributions.get(entry.info.id());
    if (contributions != null) {
      for (TraitContribution contribution : contributions) {
        effectiveTraits.addAll(contribution.traits);
      }
    }
    entry.effectiveTraits = Set.copyOf(effectiveTraits);
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
                    (RegisteredPerspective perspective) -> perspective.info.priority())
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
