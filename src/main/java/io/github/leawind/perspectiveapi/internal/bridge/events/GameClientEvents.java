package io.github.leawind.perspectiveapi.internal.bridge.events;

import io.github.leawind.perspectiveapi.internal.utils.event.SingleEventEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/// Global event emitters for client-side game events.
public final class GameClientEvents {
  private GameClientEvents() {}

  public static final SingleEventEmitter<Minecraft> CLIENT_TICK_START = new SingleEventEmitter<>();

  public static final SingleEventEmitter<ClientLevel> AFTER_CLIENT_LEVEL_CHANGE =
      new SingleEventEmitter<>();

  public static final SingleEventEmitter<Minecraft> HANDLE_KEYBINDS_START =
      new SingleEventEmitter<>();

  public static final SingleEventEmitter<CameraSetupContext> SETUP_CAMERA =
      new SingleEventEmitter<>();

  public static final SingleEventEmitter<ModifyFieldOfViewContext> MODIFY_FIELD_OF_VIEW =
      new SingleEventEmitter<>();
}
