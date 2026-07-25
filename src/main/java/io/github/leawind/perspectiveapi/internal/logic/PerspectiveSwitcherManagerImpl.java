package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherManager;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.utils.Exceptions;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class PerspectiveSwitcherManagerImpl
    implements PerspectiveSwitcherManager, Supplier<@Nullable String> {
  private static final ExtensionInvoker EXTENSIONS =
      new ExtensionInvoker(
          LoggerFactory.getLogger(PerspectiveSwitcherManagerImpl.class), "Switcher");

  public static final String KEY = PerspectiveAPI.MOD_ID + ".builtin_switcher_manager";
  private final Collection<PerspectiveSwitcherBehavior> switchers = new HashSet<>();

  private final PerspectiveSwitcherBehavior defaultSwitcher;
  private @Nullable PerspectiveSwitcherBehavior currentSwitcher = null;

  public PerspectiveSwitcherManagerImpl(@NonNull PerspectiveSwitcherBehavior defaultSwitcher) {
    this.defaultSwitcher = Objects.requireNonNull(defaultSwitcher);
    PerspectiveRegistryImpl.INSTANCE.onUpdate().on(this::notifySwitchables);
    register(defaultSwitcher);
  }

  private @NonNull List<@NonNull Perspective> getSwitchables() {
    return PerspectiveRegistryImpl.INSTANCE.getAll().stream()
        .filter(Perspective::switchable)
        .sorted(Comparator.comparingInt(Perspective::priority).thenComparing(Perspective::id))
        .toList();
  }

  private void notifySwitchables() {
    var switchables = getSwitchables();
    this.switchers.forEach(switcher -> notifySwitchables(switcher, switchables));
  }

  private void notifySwitchables(
      @NonNull PerspectiveSwitcherBehavior switcher,
      @NonNull List<@NonNull Perspective> switchables) {
    EXTENSIONS.run(
        switcher.getClass().getName(),
        "onUpdateSwitchables",
        () -> switcher.onUpdateSwitchables(switchables));
  }

  public void register(@NonNull PerspectiveSwitcherBehavior switcher) {
    Objects.requireNonNull(switcher);
    if (!switchers.add(switcher)) return;
    try {
      switcher.init();
    } catch (Throwable throwable) {
      Exceptions.rethrowIfFatal(throwable);
      switchers.remove(switcher);
      throw Exceptions.propagate(throwable);
    }
    notifySwitchables(switcher, getSwitchables());
  }

  @Override
  public @NonNull List<@NonNull PerspectiveSwitcher> getAvailableSwitchers() {
    return switchers.stream().map(switcher -> (PerspectiveSwitcher) switcher).toList();
  }

  @Override
  public @NonNull PerspectiveSwitcherBehavior getSelectedSwitcher() {
    var currentSwitcher = this.currentSwitcher;
    if (currentSwitcher == null) {
      currentSwitcher = this.currentSwitcher = defaultSwitcher;
      Perspective current = PerspectiveManager.INSTANCE.getCurrent();
      PerspectiveSwitcherBehavior activated = currentSwitcher;
      EXTENSIONS.run(
          activated.getClass().getName(), "onActivated", () -> activated.onActivated(current));
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
      throw new IllegalArgumentException(
          "Switcher is not registered: " + switcher);
    }

    if (!switchers.contains(behavior)) {
      throw new IllegalArgumentException("Unregistered switcher: " + behavior);
    }

    var old = this.currentSwitcher;
    if (old != behavior) {
      if (old != null) {
        EXTENSIONS.run(old.getClass().getName(), "onDeactivated", old::onDeactivated);
      }
      this.currentSwitcher = behavior;
      Perspective current = PerspectiveManager.INSTANCE.getCurrent();
      EXTENSIONS.run(
          behavior.getClass().getName(), "onActivated", () -> behavior.onActivated(current));
    }
  }

  @Override
  public @Nullable String get() {
    PerspectiveSwitcherBehavior switcher = getSelectedSwitcher();
    return EXTENSIONS.callOrElse(
        switcher.getClass().getName(), "getSelected", switcher::getSelected, null);
  }

  void clientTick(@NonNull Minecraft minecraft) {
    PerspectiveSwitcherBehavior switcher = getSelectedSwitcher();
    EXTENSIONS.run(
        switcher.getClass().getName(),
        "clientTickWhenActive",
        () -> switcher.clientTickWhenActive(minecraft));
  }
}
