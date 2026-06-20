package io.github.leawind.perspectiveapi.api;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Manages the lifecycle and state of camera perspectives.
///
/// Obtain via {@link PerspectiveAPI#getManager()}.
public interface PerspectiveManager {

  /// @return the perspective registry.
  @NonNull PerspectiveRegistry registry();

  /// @return the perspective cycler for cycling through perspectives.
  @NonNull PerspectiveCycler cycler();

  /// @return the transition controller.
  @NonNull Transition transition();

  @NonNull Identifier getDefault();

  /// Returns the id of the active perspective, or `null` if the default is active.
  @Nullable Identifier getActive();

  /// Sets the active perspective by id. Pass `null` to revert to the default perspective.
  void setActive(@Nullable Identifier identifier);

  /// Returns the current active perspective. Never returns `null`.
  @NonNull Perspective getCurrentPerspective();

  /// Returns the default perspective.
  default @NonNull Perspective getDefaultPerspective() {
    Perspective perspective = registry().get(getDefault());
    assert perspective != null;
    return perspective;
  }

  /// Cycles to the next available perspective in the cycle list.
  default void switchToNextAvailable() {
    var active = getActive();
    setActive(active == null ? cycler().getFirst() : cycler().getNextAvailable(registry(), active));
  }

  /// Cycles to the previous available perspective in the cycle list.
  default void switchToPreviousAvailable() {
    var active = getActive();
    setActive(
        active == null ? cycler().getFirst() : cycler().getPreviousAvailable(registry(), active));
  }
}
