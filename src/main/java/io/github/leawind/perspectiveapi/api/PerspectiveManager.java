package io.github.leawind.perspectiveapi.api;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface PerspectiveManager {
  static @NonNull PerspectiveManager get() {
    return Factory.getPerspectiveManager();
  }

  @NonNull PerspectiveRegistry registry();

  @NonNull PerspectiveCycler cycler();
  
  @NonNull Transition transition();
  
  void setDefaultPerspective(@NonNull Perspective perspective);
  
  @Nullable Identifier getActiveId();
  
  void setActive(@Nullable Identifier identifier);
  
  @NonNull Perspective getCurrentPerspective();

  @NonNull Perspective getDefaultPerspective();

  default void switchToNextAvailable() {
    var active = getActiveId();
    setActive(active == null ? cycler().getFirst() : cycler().getNextAvailable(registry(), active));
  }

  default void switchToPreviousAvailable() {
    var active = getActiveId();
    setActive(
        active == null ? cycler().getFirst() : cycler().getPreviousAvailable(registry(), active));
  }
}
