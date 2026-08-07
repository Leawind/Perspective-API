package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherManager;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.utils.Exceptions;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class PerspectiveSwitcherManagerImpl
    implements PerspectiveSwitcherManager, Supplier<@Nullable String> {
  private static final ExtensionInvoker EXTENSIONS =
      new ExtensionInvoker(
          LoggerFactory.getLogger(PerspectiveSwitcherManagerImpl.class), "Switcher");

  private final Map<String, PerspectiveSwitcherBehavior> switchers = new HashMap<>();

  private final PerspectiveSwitcherBehavior defaultSwitcher;
  private @Nullable PerspectiveSwitcherBehavior currentSwitcher = null;

  public PerspectiveSwitcherManagerImpl(@NonNull PerspectiveSwitcherBehavior defaultSwitcher) {
    this.defaultSwitcher = Objects.requireNonNull(defaultSwitcher);
    PerspectiveRegistryImpl.INSTANCE.onUpdate().on(this::notifySwitchables);
    register(defaultSwitcher);
  }

  private @NonNull List<@NonNull Perspective> getSwitchables() {
    return PerspectiveRegistryImpl.INSTANCE.getAllPerspectives().stream()
        .filter(perspective -> perspective.info().switchable())
        .sorted(
            Comparator.comparingInt((Perspective perspective) -> perspective.info().priority())
                .thenComparing(perspective -> perspective.info().id()))
        .toList();
  }

  private void notifySwitchables() {
    var switchables = getSwitchables();
    this.switchers.values().forEach(switcher -> notifySwitchables(switcher, switchables));
  }

  private void notifySwitchables(
      @NonNull PerspectiveSwitcherBehavior switcher,
      @NonNull List<@NonNull Perspective> switchables) {
    EXTENSIONS.run(
        switcher.id(),
        "onSwitchablePerspectivesUpdated",
        () -> switcher.onSwitchablePerspectivesUpdated(switchables));
  }

  public void register(@NonNull PerspectiveSwitcherBehavior switcher) {
    Objects.requireNonNull(switcher);
    String id = Objects.requireNonNull(switcher.id());
    if (id.isEmpty()) throw new IllegalArgumentException("Switcher id must not be empty");
    if (switchers.putIfAbsent(id, switcher) != null) {
      throw new IllegalArgumentException("Switcher id is already registered: '" + id + "'");
    }
    try {
      switcher.init();
    } catch (Throwable throwable) {
      Exceptions.rethrowIfFatal(throwable);
      switchers.remove(id, switcher);
      throw Exceptions.propagate(throwable);
    }
    notifySwitchables(switcher, getSwitchables());
  }

  @Override
  public @NonNull List<@NonNull PerspectiveSwitcher> getAvailableSwitchers() {
    return switchers.values().stream()
        .sorted(Comparator.comparing(PerspectiveSwitcherBehavior::id))
        .map(switcher -> (PerspectiveSwitcher) switcher)
        .toList();
  }

  @Override
  public @NonNull PerspectiveSwitcherBehavior getSelectedSwitcher() {
    var currentSwitcher = this.currentSwitcher;
    if (currentSwitcher == null) {
      currentSwitcher = this.currentSwitcher = defaultSwitcher;
      Perspective current = PerspectiveManager.INSTANCE.getLastResolvedOrDefault();
      PerspectiveSwitcherBehavior activated = currentSwitcher;
      EXTENSIONS.run(activated.id(), "onActivated", () -> activated.onActivated(current));
    }

    return currentSwitcher;
  }

  public PerspectiveSwitcherBehavior getDefault() {
    return defaultSwitcher;
  }

  @Override
  public void setSelectedSwitcher(@NonNull PerspectiveSwitcher switcher) {
    Objects.requireNonNull(switcher);
    if (!(switcher instanceof PerspectiveSwitcherBehavior behavior)) {
      throw new IllegalArgumentException("Switcher is not registered: " + switcher);
    }

    if (switchers.get(behavior.id()) != behavior) {
      throw new IllegalArgumentException("Unregistered switcher: " + behavior);
    }

    var old = this.currentSwitcher;
    if (old != behavior) {
      if (old != null) {
        EXTENSIONS.run(old.id(), "onDeactivated", old::onDeactivated);
      }
      this.currentSwitcher = behavior;
      Perspective current = PerspectiveManager.INSTANCE.getLastResolvedOrDefault();
      EXTENSIONS.run(behavior.id(), "onActivated", () -> behavior.onActivated(current));
    }
  }

  public @Nullable PerspectiveSwitcherBehavior getById(@NonNull String id) {
    return switchers.get(Objects.requireNonNull(id));
  }

  @Override
  public @Nullable String get() {
    PerspectiveSwitcherBehavior switcher = getSelectedSwitcher();
    return EXTENSIONS.callOrElse(
        switcher.id(), "getSelectedPerspectiveId", switcher::getSelectedPerspectiveId, null);
  }
}
