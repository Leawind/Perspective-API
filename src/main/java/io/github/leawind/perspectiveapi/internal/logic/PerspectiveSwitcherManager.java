package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.spi.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class PerspectiveSwitcherManager implements Supplier<String> {
  public static final Identifier KEY =
      Bridge.createIdentifier(PerspectiveAPI.MOD_ID, "builtin_switcher_manager");
  private final Collection<PerspectiveSwitcher> switchers = new HashSet<>();

  private final PerspectiveSwitcher defaultSwitcher;
  private @Nullable PerspectiveSwitcher currentSwitcher = null;

  public PerspectiveSwitcherManager(@NonNull PerspectiveSwitcher defaultSwitcher) {
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

  public void register(PerspectiveSwitcher switcher) {
    switchers.add(switcher);
    switcher.init();
  }

  public PerspectiveSwitcher getDefault() {
    return defaultSwitcher;
  }

  public @NonNull List<PerspectiveSwitcher> getSwitchers() {
    return switchers.stream().toList();
  }

  public @NonNull PerspectiveSwitcher getSwitcher() {
    var currentSwitcher = this.currentSwitcher;
    if (currentSwitcher == null) {
      currentSwitcher = this.currentSwitcher = defaultSwitcher;
      currentSwitcher.onActivated(PerspectiveManager.INSTANCE.getCurrent());
    }

    return currentSwitcher;
  }

  public void setCurrent(PerspectiveSwitcher switcher) {
    if (!switchers.contains(switcher)) {
      throw new IllegalArgumentException("Unregistered switcher: " + switcher);
    }

    var old = this.currentSwitcher;
    if (old != switcher) {
      if (old != null) {
        old.onDeactivated();
      }
      this.currentSwitcher = switcher;
      switcher.onActivated(PerspectiveManager.INSTANCE.getCurrent());
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
