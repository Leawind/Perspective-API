package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.spi.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

class DefaultSwitcher implements PerspectiveSwitcher {
  private volatile List<Identifier> list = new ArrayList<>();
  private volatile @Nullable Identifier selected = null;

  @Override
  public void init() {
    GameClientEvents.HANDLE_KEYBINDS_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()) return;
          if (minecraft.level == null || minecraft.player == null) return;
          if (PerspectiveManager.INSTANCE.switchers().getSwitcher() == this) {
            while (minecraft.options.keyTogglePerspective.consumeClick()) {
              cycleForward();
            }
          }
        });
  }

  @Override
  public @NonNull Component getNameComponent() {
    return Component.literal("perspective_api.default_switcher.name");
  }

  @Override
  public @NonNull Component getDescriptionComponent() {
    return Component.literal("perspective_api.default_switcher.description");
  }

  @Override
  public void onActivated(Context context) {}

  @Override
  public void clientTickWhenActive(Context context) {
    this.list = context.getSwitchable().stream().toList();
  }

  @Override
  public void onDeactivated(Context context) {}

  @Override
  public @Nullable Identifier getSelected() {
    return selected;
  }

  /// Advances the active perspective to the next available one.
  private synchronized void cycleForward() {
    if (list.isEmpty()) return;

    Identifier current = getSelected();
    int idx = list.indexOf(current);
    int start = idx < 0 ? 0 : (idx + 1) % list.size();

    int attempts = 0;
    int size = list.size();
    int i = start;
    do {
      Identifier next = list.get(i);
      var p = PerspectiveManager.INSTANCE.registry().get(next);
      if (p != null && p.isAvailable()) {
        selected = next;
        return;
      }
      i = (i + 1) % size;
      attempts++;
    } while (attempts < size);
  }

  /// Moves the active perspective to the previous available one.
  private synchronized void cycleBackward() {
    if (list.isEmpty()) return;

    Identifier current = getSelected();
    int idx = list.indexOf(current);
    int size = list.size();
    int i = idx < 0 ? size - 1 : (idx - 1 + size) % size;

    int attempts = 0;
    do {
      Identifier next = list.get(i);
      var p = PerspectiveManager.INSTANCE.registry().get(next);
      if (p != null && p.isAvailable()) {
        selected = next;
        return;
      }
      i = (i - 1 + size) % size;
      attempts++;
    } while (attempts < size);
  }
}
