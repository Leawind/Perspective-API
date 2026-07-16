package io.github.leawind.perspectiveapi.api.spi;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcher;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface PerspectiveSwitcherBehavior extends PerspectiveSwitcher {

  default void init() {}

  void onUpdateSwitchables(@NonNull List<Perspective> switchables);

  void onActivated(@NonNull Perspective currentPerspective);

  void clientTickWhenActive();

  void onDeactivated();

  @Nullable String getSelected();
}
