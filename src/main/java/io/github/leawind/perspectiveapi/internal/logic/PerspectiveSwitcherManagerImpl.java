package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherManager;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PerspectiveSwitcherManagerImpl
    implements PerspectiveSwitcherManager, Supplier<String> {
  public static final String KEY = PerspectiveAPI.MOD_ID + ".builtin_switcher_manager";
  private final Collection<PerspectiveSwitcherBehavior> switchers = new HashSet<>();

  private final PerspectiveSwitcherBehavior defaultSwitcher;
  private @Nullable PerspectiveSwitcherBehavior currentSwitcher = null;

  public PerspectiveSwitcherManagerImpl(@NonNull PerspectiveSwitcherBehavior defaultSwitcher) {
    this.defaultSwitcher = defaultSwitcher;
    register(defaultSwitcher);

    PerspectiveRegistryImpl.INSTANCE.onUpdate().on(this::notifySwitchables);
    notifySwitchables();
  }

  private void notifySwitchables() {
    var switchers =
        PerspectiveRegistryImpl.INSTANCE.getAll().stream()
            .filter(Perspective::switchable)
            .sorted(Comparator.comparingInt(Perspective::priority))
            .toList();
    this.switchers.forEach(switcher -> switcher.onUpdateSwitchables(switchers));
  }

  public void register(PerspectiveSwitcherBehavior switcher) {
    switchers.add(switcher);
    switcher.init();
  }

  @Override
  public @NonNull List<PerspectiveSwitcher> getSwitchers() {
    return new ArrayList<>(switchers);
  }

  @Override
  public @NonNull PerspectiveSwitcherBehavior getSwitcher() {
    var currentSwitcher = this.currentSwitcher;
    if (currentSwitcher == null) {
      currentSwitcher = this.currentSwitcher = defaultSwitcher;
      currentSwitcher.onActivated(PerspectiveManager.INSTANCE.getCurrent());
    }

    return currentSwitcher;
  }

  public PerspectiveSwitcherBehavior getDefault() {
    return defaultSwitcher;
  }

  @Override
  public void setSwitcher(@NonNull PerspectiveSwitcher switcher) {
    if (!(switcher instanceof PerspectiveSwitcherBehavior behavior)) {
      throw new IllegalArgumentException(
          "Expect switcher to implement "
              + PerspectiveSwitcherBehavior.class
              + ", but got "
              + switcher.getClass());
    }

    if (!switchers.contains(behavior)) {
      throw new IllegalArgumentException("Unregistered switcher: " + behavior);
    }

    var old = this.currentSwitcher;
    if (old != behavior) {
      if (old != null) {
        old.onDeactivated();
      }
      this.currentSwitcher = behavior;
      behavior.onActivated(PerspectiveManager.INSTANCE.getCurrent());
    }
  }

  @Override
  public @Nullable String get() {
    return getSwitcher().getSelected();
  }

  void clientTick() {
    getSwitcher().clientTickWhenActive();
  }
}
