package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import com.mojang.serialization.Codec;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.logic.state.PerspectiveAPIState;
import io.github.leawind.perspectiveapi.internal.utils.KeyStateTracker;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class OrbitSwitcherBehavior
    implements PerspectiveSwitcherBehavior, PerspectiveAPIState.Section<OrbitSwitcherState> {
  public static final String ID = PerspectiveAPI.MOD_ID + ".orbit_switcher";
  public static final OrbitSwitcherBehavior INSTANCE;

  static {
    INSTANCE = new OrbitSwitcherBehavior();
    PerspectiveAPIState.registerSection(INSTANCE);
  }

  private final OrbitSwitcherModel model = new OrbitSwitcherModel();
  private final KeyStateTracker keyStateTracker;
  private final OrbitMenu menu = new OrbitMenu(this, model);

  private OrbitSwitcherBehavior() {
    keyStateTracker =
        KeyStateTracker.builder()
            .setHoldTicks(3)
            .onPress(model::cycleForward)
            .onHoldStart(menu::open)
            .onHoldStop(menu::closeFromKey)
            .build();
  }

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public void init() {
    GameClientEvents.HANDLE_KEYBINDS_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()) return;
          if (PerspectiveAPI.getSwitcherManager().getSelectedSwitcher() != this) return;

          KeyMapping key = minecraft.options.keyTogglePerspective;
          keyStateTracker.tick(key.isDown());
          while (key.consumeClick()) {}
        });
    menu.init();
  }

  @Override
  public @NonNull String id() {
    return ID;
  }

  @Override
  public @NonNull Component name() {
    return Component.translatable("perspective_api.switcher.orbit_switcher.name");
  }

  @Override
  public @NonNull Component description() {
    return Component.translatable("perspective_api.switcher.orbit_switcher.description");
  }

  @Override
  public void onSwitchablePerspectivesUpdated(
      @NonNull List<@NonNull Perspective> switchablePerspectives) {
    model.updateSwitchables(switchablePerspectives);
    menu.syncActors();
  }

  @Override
  public void onActivated(@NonNull Perspective currentPerspective) {
    model.activate(currentPerspective.info().id());
    model.ensureActive();
  }

  @Override
  public void clientTickWhenActive(@NonNull Minecraft minecraft) {
    model.ensureActive();
    if (menu.isOpened() && Bridge.getScreen(minecraft) != null) menu.close();
  }

  @Override
  public void onDeactivated() {
    keyStateTracker.reset();
    menu.close();
    model.clearPreview();
  }

  @Override
  public @Nullable String getSelectedPerspectiveId() {
    model.ensureActive();
    return model.resolvedId();
  }

  @Override
  public @NonNull String stateId() {
    return id();
  }

  @Override
  public @NonNull Codec<OrbitSwitcherState> stateCodec() {
    return OrbitSwitcherState.CODEC;
  }

  @Override
  public @NonNull OrbitSwitcherState extractState() {
    return new OrbitSwitcherState(model.selected(), model.disabled());
  }

  @Override
  public void applyState(@NonNull OrbitSwitcherState state) {
    Objects.requireNonNull(state);
    model.applyLayout(state.selected(), state.disabled());
    model.ensureActive();
    menu.syncActors();
  }

  OrbitSwitcherModel model() {
    return model;
  }

  KeyStateTracker keyStateTracker() {
    return keyStateTracker;
  }

  OrbitMenu menu() {
    return menu;
  }
}
