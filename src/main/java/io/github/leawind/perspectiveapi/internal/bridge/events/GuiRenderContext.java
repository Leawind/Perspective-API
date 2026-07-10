package io.github.leawind.perspectiveapi.internal.bridge.events;

import io.github.leawind.perspectiveapi.internal.bridge.gui.DrawContext;

/// Context for GUI render events.
public class GuiRenderContext {
  public DrawContext drawContext;
  public int screenWidth;
  public int screenHeight;

  public void setup(DrawContext drawContext, int screenWidth, int screenHeight) {
    this.drawContext = drawContext;
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
  }
}
