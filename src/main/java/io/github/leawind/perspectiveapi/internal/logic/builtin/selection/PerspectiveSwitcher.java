package io.github.leawind.perspectiveapi.internal.logic.builtin.selection;

import com.mojang.serialization.Codec;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.logic.state.PerspectiveAPIState;
import io.github.leawind.perspectiveapi.internal.utils.KeyStateTracker;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.KeyMapping;
import org.jspecify.annotations.NonNull;

public final class PerspectiveSwitcher
    implements PerspectiveAPIState.Section<PerspectiveSwitcherState> {
  public static final String ID = PerspectiveAPI.MOD_ID + ".orbit_switcher";
  public static final int DEFAULT_HOLD_TICKS = 3;
  public static final int MIN_HOLD_TICKS = 0;
  public static final int MAX_HOLD_TICKS = 20;
  public static final PerspectiveSwitcher INSTANCE;

  static {
    INSTANCE = new PerspectiveSwitcher();
    PerspectiveAPIState.registerSection(INSTANCE);
  }

  private final OrbitMenuModel model = new OrbitMenuModel();
  private final OrbitMenuTutorial tutorial = new OrbitMenuTutorial();
  private final KeyStateTracker keyStateTracker;
  private final OrbitMenu menu = new OrbitMenu(this, model);

  private PerspectiveSwitcher() {
    keyStateTracker =
        KeyStateTracker.builder()
            .setHoldTicks(DEFAULT_HOLD_TICKS)
            .onPress(this::onPress)
            .onHoldStart(menu::open)
            .onHoldStop(menu::closeFromKey)
            .build();
  }

  @SuppressWarnings("StatementWithEmptyBody")
  public void init() {
    PerspectiveRegistryImpl.INSTANCE.onUpdate().on(this::updateSwitchables);
    updateSwitchables();
    GameClientEvents.HANDLE_KEYBINDS_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()) return;

          KeyMapping key = minecraft.options.keyTogglePerspective;
          keyStateTracker.tick(key.isDown());
          while (key.consumeClick()) {}
        });
    GameClientEvents.CLIENT_TICK_START.on(
        minecraft -> {
          if (!PerspectiveAPI.isEnabled()
              || minecraft.level == null
              || minecraft.player == null) return;
          if (menu.isOpened() && Bridge.getScreen(minecraft) != null) menu.close();
        });
    menu.init();
  }

  private void updateSwitchables() {
    List<Perspective> switchables =
        PerspectiveRegistryImpl.INSTANCE.getAllPerspectives().stream()
            .filter(perspective -> perspective.info().hasTrait("switchable"))
            .toList();
    updateSwitchables(switchables);
  }

  void updateSwitchables(@NonNull List<@NonNull Perspective> switchables) {
    model.updateSwitchables(switchables);
    menu.syncActors();
  }

  public void deactivate() {
    keyStateTracker.reset();
    menu.close();
    tutorial.onWheelClosed();
  }

  @Override
  public @NonNull String stateId() {
    return ID;
  }

  @Override
  public @NonNull Codec<PerspectiveSwitcherState> stateCodec() {
    return PerspectiveSwitcherState.CODEC;
  }

  @Override
  public @NonNull PerspectiveSwitcherState extractState() {
    return new PerspectiveSwitcherState(
        model.selected(),
        model.disabled(),
        getHoldTicks(),
        tutorial.wheelHintCompleted(),
        tutorial.editorHintCompleted());
  }

  @Override
  public void applyState(@NonNull PerspectiveSwitcherState state) {
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
    Perspective next = model.cycleForward(selection.get());
    if (next != null) selection.set(next.info().id());
    tutorial.onShortPress();
  }

  OrbitMenuModel model() {
    return model;
  }

  KeyStateTracker keyStateTracker() {
    return keyStateTracker;
  }

  OrbitMenu menu() {
    return menu;
  }
}
