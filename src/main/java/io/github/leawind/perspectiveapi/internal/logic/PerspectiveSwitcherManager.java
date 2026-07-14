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
  private @NonNull PerspectiveSwitcher switcher;
  private final SwitcherContext context = new SwitcherContext();

  public PerspectiveSwitcherManager(@NonNull PerspectiveSwitcher defaultSwitcher) {
    this.defaultSwitcher = defaultSwitcher;
    register(defaultSwitcher);

    this.switcher = defaultSwitcher;
  }

  public void register(PerspectiveSwitcher switcher) {
    switchers.add(switcher);
    switcher.init();
  }

  public PerspectiveSwitcher getDefault() {
    return defaultSwitcher;
  }

  public @NonNull PerspectiveSwitcher getSwitcher() {
    return switcher;
  }

  public void setSwitcher(PerspectiveSwitcher switcher) {
    if (!switchers.contains(switcher)) {
      throw new IllegalArgumentException("Unregistered switcher: " + switcher);
    }

    var old = this.switcher;
    if (old != switcher) {
      old.onDeactivated(context);
      this.switcher = switcher;
      switcher.onActivated(context);
    }
  }

  @Override
  public @Nullable String get() {
    return getSwitcher().getSelected();
  }

  void clientTick() {
    context.setup(
        PerspectiveRegistryImpl.INSTANCE.getAll().stream()
            .filter(Perspective::isSwitchable)
            .sorted(Comparator.comparingInt(Perspective::priority))
            .map(Perspective::id)
            .toList());
    getSwitcher().clientTickWhenActive(context);
  }

  private static class SwitcherContext implements PerspectiveSwitcher.Context {

    private List<String> switchable;

    void setup(List<String> switchable) {
      this.switchable = switchable;
    }

    @Override
    public List<String> getSwitchable() {
      return switchable;
    }
  }
}
