package io.github.leawind.perspectiveapi.internal.bridge.events;

import io.github.leawind.inventory.event.SingleEventEmitter;
import io.github.leawind.perspectiveapi.internal.bridge.events.context.CameraSetupContext;
import io.github.leawind.perspectiveapi.internal.bridge.events.context.ModifyFieldOfViewContext;
import net.minecraft.client.Minecraft;

public final class GameClientEvents {
  private GameClientEvents() {}

  public static final SingleEventEmitter<Minecraft> CLIENT_TICK_START = new SingleEventEmitter<>();

  public static final SingleEventEmitter<Minecraft> HANDLE_KEYBINDS_START =
      new SingleEventEmitter<>();

  public static final SingleEventEmitter<CameraSetupContext> SETUP_CAMERA =
      new SingleEventEmitter<>();

  public static final SingleEventEmitter<ModifyFieldOfViewContext> MODIFY_FIELD_OF_VIEW =
      new SingleEventEmitter<>();
}
