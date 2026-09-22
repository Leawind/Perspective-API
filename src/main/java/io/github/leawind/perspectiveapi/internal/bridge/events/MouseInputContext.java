package io.github.leawind.perspectiveapi.internal.bridge.events;

import com.mojang.blaze3d.platform.InputConstants;

/// Context for mouse input events intercepted by bridge mixins.
///
/// Vanilla reports mouse buttons and actions with the codes of the windowing backend it currently
/// uses, and those codes differ between Minecraft versions. This class translates them once, so
/// listeners only see the backend independent {@link Button} and {@link Action} values.
public class MouseInputContext {
  public enum Type {
    BUTTON,
    SCROLL,
    MOVE
  }

  public enum Button {
    LEFT,
    MIDDLE,
    RIGHT,
    OTHER
  }

  public enum Action {
    PRESS,
    RELEASE,
    REPEAT,
    OTHER
  }

  public Type type;
  public Button button;
  public Action action;
  public double scrollDelta;
  public double mouseX;
  public double mouseY;
  /// Whether the event was consumed by a listener.
  public boolean consumed;

  public void setupButton(int button, int action, double mouseX, double mouseY) {
    this.type = Type.BUTTON;
    this.button = translateButton(button);
    this.action = translateAction(action);
    this.mouseX = mouseX;
    this.mouseY = mouseY;
    this.consumed = false;
  }

  public void setupScroll(double scrollDelta) {
    this.type = Type.SCROLL;
    this.scrollDelta = scrollDelta;
    this.consumed = false;
  }

  public void setupMove(double mouseX, double mouseY) {
    this.type = Type.MOVE;
    this.mouseX = mouseX;
    this.mouseY = mouseY;
    this.consumed = false;
  }

  private static Button translateButton(int button) {
    if (button == InputConstants.MOUSE_BUTTON_LEFT) return Button.LEFT;
    if (button == InputConstants.MOUSE_BUTTON_MIDDLE) return Button.MIDDLE;
    if (button == InputConstants.MOUSE_BUTTON_RIGHT) return Button.RIGHT;
    return Button.OTHER;
  }

  private static Action translateAction(int action) {
    if (action == InputConstants.PRESS) return Action.PRESS;
    if (action == InputConstants.RELEASE) return Action.RELEASE;
    if (action == InputConstants.REPEAT) return Action.REPEAT;
    return Action.OTHER;
  }
}
