package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherManager;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.utils.Exceptions;
import io.github.leawind.perspectiveapi.internal.utils.Sanitizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerspectiveSwitcherManagerImpl
    implements PerspectiveSwitcherManager, Supplier<@Nullable String> {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(PerspectiveSwitcherManagerImpl.class);
  private static final Sanitizer.ThrottledAction CALLBACK_EXCEPTION_LOG =
      new Sanitizer.ThrottledAction(5000);

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
    try {
      switcher.onUpdateSwitchables(switchables);
    } catch (Throwable throwable) {
      reportException(switcher, "onUpdateSwitchables", throwable);
    }
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
  public @NonNull List<@NonNull PerspectiveSwitcher> getSwitchers() {
    return new ArrayList<>(switchers);
  }

  @Override
  public @NonNull PerspectiveSwitcherBehavior getSwitcher() {
    var currentSwitcher = this.currentSwitcher;
    if (currentSwitcher == null) {
      currentSwitcher = this.currentSwitcher = defaultSwitcher;
      try {
        currentSwitcher.onActivated(PerspectiveManager.INSTANCE.getCurrent());
      } catch (Throwable throwable) {
        reportException(currentSwitcher, "onActivated", throwable);
      }
    }

    return currentSwitcher;
  }

  public PerspectiveSwitcherBehavior getDefault() {
    return defaultSwitcher;
  }

  @Override
  public void setSwitcher(@NonNull PerspectiveSwitcher switcher) {
    Objects.requireNonNull(switcher);
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
        try {
          old.onDeactivated();
        } catch (Throwable throwable) {
          reportException(old, "onDeactivated", throwable);
        }
      }
      this.currentSwitcher = behavior;
      try {
        behavior.onActivated(PerspectiveManager.INSTANCE.getCurrent());
      } catch (Throwable throwable) {
        reportException(behavior, "onActivated", throwable);
      }
    }
  }

  @Override
  public @Nullable String get() {
    return getSwitcher().getSelected();
  }

  void clientTick(@NonNull Minecraft minecraft) {
    PerspectiveSwitcherBehavior switcher = getSwitcher();
    try {
      switcher.clientTickWhenActive(minecraft);
    } catch (Throwable throwable) {
      reportException(switcher, "clientTickWhenActive", throwable);
    }
  }

  private static void reportException(
      @NonNull PerspectiveSwitcherBehavior switcher,
      @NonNull String phase,
      @NonNull Throwable throwable) {
    Exceptions.rethrowIfFatal(throwable);
    String id = switcher.getClass().getName();
    CALLBACK_EXCEPTION_LOG.run(
        id + ":" + phase, () -> LOGGER.warn("Switcher '{}' threw during {}", id, phase, throwable));
  }
}
