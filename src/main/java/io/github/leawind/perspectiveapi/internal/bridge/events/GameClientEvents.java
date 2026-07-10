package io.github.leawind.perspectiveapi.internal.bridge.events;

import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import io.github.leawind.perspectiveapi.internal.utils.event.SingleEventEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/// Global event emitters for client-side game events.
public final class GameClientEvents {
  private GameClientEvents() {}

  public static final SingleEventEmitter<Minecraft> AFTER_MINECRAFT_INIT =
      new SingleEventEmitter<>();

  public static final SingleEventEmitter<Minecraft> ON_MINECRAFT_CLOSE = new SingleEventEmitter<>();

  public static final SimpleEventEmitter.Owned<Minecraft> CLIENT_TICK_START =
      SimpleEventEmitter.create();

  public static final SingleEventEmitter<ClientLevel> AFTER_CLIENT_LEVEL_CHANGE =
      new SingleEventEmitter<>();

  public static final SimpleEventEmitter.Owned<Minecraft> HANDLE_KEYBINDS_START =
      SimpleEventEmitter.create();

  public static final SingleEventEmitter<CameraSetupContext> SETUP_CAMERA =
      new SingleEventEmitter<>();

  public static final SingleEventEmitter<ModifyFieldOfViewContext> MODIFY_FIELD_OF_VIEW =
      new SingleEventEmitter<>();

  public static final SingleEventEmitter<GuiRenderContext> RENDER_GUI_OVERLAY =
      new SingleEventEmitter<>();
  public static final SingleEventEmitter<MouseInputContext> MOUSE_INPUT =
      new SingleEventEmitter<>();
}
