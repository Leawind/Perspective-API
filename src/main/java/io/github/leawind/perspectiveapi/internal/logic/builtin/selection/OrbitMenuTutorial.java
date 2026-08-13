package io.github.leawind.perspectiveapi.internal.logic.builtin.selection;

import io.github.leawind.perspectiveapi.internal.bridge.gui.TutorialHint;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

final class OrbitMenuTutorial {
  private boolean wheelHintCompleted;
  private boolean editorHintCompleted;
  private @Nullable TutorialHint visibleHint;

  void onShortPress() {
    if (wheelHintCompleted || visibleHint != null) return;
    visibleHint =
        TutorialHint.show(
            TutorialHint.Icon.MOUSE,
            Component.translatable("perspective_api.switcher.orbit_switcher.tutorial.wheel.title"),
            Component.translatable(
                "perspective_api.switcher.orbit_switcher.tutorial.wheel.description",
                Minecraft.getInstance().options.keyTogglePerspective.getTranslatedKeyMessage()));
  }

  void onWheelOpened() {
    wheelHintCompleted = true;
    hide();
    if (editorHintCompleted) return;
    visibleHint =
        TutorialHint.show(
            TutorialHint.Icon.RIGHT_CLICK,
            Component.translatable("perspective_api.switcher.orbit_switcher.tutorial.editor.title"),
            Component.translatable(
                "perspective_api.switcher.orbit_switcher.tutorial.editor.description"));
  }

  void onEditingOpened() {
    editorHintCompleted = true;
    hide();
  }

  void onWheelClosed() {
    hide();
  }

  void applyState(boolean wheelHintCompleted, boolean editorHintCompleted) {
    this.wheelHintCompleted = wheelHintCompleted;
    this.editorHintCompleted = editorHintCompleted;
    hide();
  }

  boolean wheelHintCompleted() {
    return wheelHintCompleted;
  }

  boolean editorHintCompleted() {
    return editorHintCompleted;
  }

  private void hide() {
    if (visibleHint == null) return;
    visibleHint.hide();
    visibleHint = null;
  }
}
