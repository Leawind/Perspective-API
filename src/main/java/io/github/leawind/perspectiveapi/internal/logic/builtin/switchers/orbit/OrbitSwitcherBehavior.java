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
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public final class OrbitSwitcherBehavior
    implements PerspectiveSwitcherBehavior, PerspectiveAPIState.Section<OrbitSwitcherState> {
  public static final String ID = PerspectiveAPI.MOD_ID + ".orbit_switcher";
  public static final int DEFAULT_HOLD_TICKS = 3;
  public static final int MIN_HOLD_TICKS = 0;
  public static final int MAX_HOLD_TICKS = 20;
  public static final OrbitSwitcherBehavior INSTANCE;

  static {
    INSTANCE = new OrbitSwitcherBehavior();
    PerspectiveAPIState.registerSection(INSTANCE);
  }

  private final OrbitSwitcherModel model = new OrbitSwitcherModel();
  private final OrbitSwitcherTutorial tutorial = new OrbitSwitcherTutorial();
  private final KeyStateTracker keyStateTracker;
  private final OrbitMenu menu = new OrbitMenu(this, model);

  private OrbitSwitcherBehavior() {
    keyStateTracker =
        KeyStateTracker.builder()
            .setHoldTicks(DEFAULT_HOLD_TICKS)
            .onPress(this::onPress)
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
    GameClientEvents.CLIENT_TICK_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()
              || minecraft.level == null
              || minecraft.player == null
              || PerspectiveAPI.getSwitcherManager().getSelectedSwitcher() != this) return;
          if (menu.isOpened() && Bridge.getScreen(minecraft) != null) menu.close();
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
  public void onDeactivated() {
    keyStateTracker.reset();
    menu.close();
    tutorial.onWheelClosed();
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
    return new OrbitSwitcherState(
        model.selected(),
        model.disabled(),
        getHoldTicks(),
        tutorial.wheelHintCompleted(),
        tutorial.editorHintCompleted());
  }

  @Override
  public void applyState(@NonNull OrbitSwitcherState state) {
    Objects.requireNonNull(state);
    setHoldTicks(state.holdTicks());
    tutorial.applyState(state.wheelHintCompleted(), state.editorHintCompleted());
    model.applyLayout(state.selected(), state.disabled());
    menu.syncActors();
  }

  public int getHoldTicks() {
    return keyStateTracker.getHoldTicks();
  }

  public void setHoldTicks(int holdTicks) {
    keyStateTracker.setHoldTicks(validateHoldTicks(holdTicks));
  }

  static int validateHoldTicks(int holdTicks) {
    if (holdTicks < MIN_HOLD_TICKS || holdTicks > MAX_HOLD_TICKS) {
      throw new IllegalArgumentException(
          "holdTicks must be between " + MIN_HOLD_TICKS + " and " + MAX_HOLD_TICKS);
    }
    return holdTicks;
  }

  void onMenuOpened() {
    tutorial.onWheelOpened();
  }

  void onEditingOpened() {
    tutorial.onEditingOpened();
  }

  void onMenuClosed() {
    tutorial.onWheelClosed();
  }

  private void onPress() {
    var selection = PerspectiveAPI.getSelection();
    Perspective next = model.cycleForward(selection.selectedPerspectiveId());
    if (next != null) selection.setSelectedPerspective(next);
    tutorial.onShortPress();
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
