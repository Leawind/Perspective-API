package io.github.leawind.perspectiveapi.api.spi;

import java.util.List;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface PerspectiveSwitcher {
  default void init() {}

  @NonNull Component getNameComponent();

  default @Nullable Component getDescriptionComponent() {
    return null;
  }

  void onActivated(Context context);

  void clientTickWhenActive(Context context);

  void onDeactivated(Context context);

  @Nullable String getSelected();

  interface Context {
    List<String> getSwitchable();
  }
}
