package io.github.leawind.perspectiveapi.api.spi;

import io.github.leawind.perspectiveapi.api.PerspectiveMeta;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface PerspectiveSwitcher {

  // region meta
  @NonNull Component getNameComponent();

  default @Nullable Component getDescriptionComponent() {
    return null;
  }

  // endregion

  // region events

  default void init() {}

  void onUpdateSwitchables(@NonNull List<PerspectiveMeta> switchables);

  void onActivated(@NonNull PerspectiveMeta currentPerspectiveMeta);

  void clientTickWhenActive();

  void onDeactivated();

  // endregion

  @Nullable String getSelected();
}
