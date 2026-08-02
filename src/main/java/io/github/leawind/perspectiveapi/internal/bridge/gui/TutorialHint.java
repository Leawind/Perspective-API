package io.github.leawind.perspectiveapi.internal.bridge.gui;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// A version-independent handle for a vanilla-style tutorial toast.
public final class TutorialHint {
  public enum Icon {
    MOUSE,
    RIGHT_CLICK
  }

  private final TutorialToast toast;

  private TutorialHint(TutorialToast toast) {
    this.toast = toast;
  }

  public static @NonNull TutorialHint show(
      @NonNull Icon icon, @NonNull Component title, @Nullable Component message) {
    Objects.requireNonNull(icon);
    Objects.requireNonNull(title);
    Minecraft minecraft = Minecraft.getInstance();
    TutorialToast.Icons vanillaIcon =
        switch (icon) {
          case MOUSE -> TutorialToast.Icons.MOUSE;
          case RIGHT_CLICK -> TutorialToast.Icons.RIGHT_CLICK;
        };

    TutorialToast toast;
    /*? if >=26.2 {*/
    toast = new TutorialToast(minecraft.font, vanillaIcon, title, message, false);
    minecraft.gui.toastManager().addToast(toast);
    /*? } else if >=1.21.11 {*/
    /*toast = new TutorialToast(minecraft.font, vanillaIcon, title, message, false);
    minecraft.getToastManager().addToast(toast);
    *//*? } else {*/
    /*toast = new TutorialToast(vanillaIcon, title, message, false);
    minecraft.getToasts().addToast(toast);
    *//*? }*/
    return new TutorialHint(toast);
  }

  public void hide() {
    toast.hide();
  }
}
