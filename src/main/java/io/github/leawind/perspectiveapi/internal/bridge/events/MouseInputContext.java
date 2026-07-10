package io.github.leawind.perspectiveapi.internal.bridge.events;

/// Context for mouse input events intercepted by bridge mixins.
public class MouseInputContext {
  public enum Type {
    BUTTON,
    SCROLL,
    MOVE
  }

  public Type type;
  public int button;
  public int action;
  public double scrollDelta;
  public double mouseX;
  public double mouseY;
  /// Whether the event was consumed by a listener.
  public boolean consumed;

  public void setupButton(int button, int action) {
    this.type = Type.BUTTON;
    this.button = button;
    this.action = action;
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
}
