package io.github.leawind.perspectiveapi.api;

import io.github.leawind.inventory.event.EventEmitter;
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

  /// Sets the default perspective used when no active perspective is set or the active one is
  /// unregistered.
  void setDefaultPerspective(@NonNull Perspective perspective);

  /// Returns the id of the active perspective, or `null` if the default is active.
  @Nullable Identifier getActiveId();

  /// Sets the active perspective by id. Pass `null` to revert to the default perspective.
  void setActive(@Nullable Identifier identifier);

  /// Returns the current active perspective. Never returns `null`.
  @NonNull Perspective getCurrentPerspective();

  /// Returns the default perspective.
  @NonNull Perspective getDefaultPerspective();

  /// Cycles to the next available perspective in the cycle list.
  default void switchToNextAvailable() {
    var active = getActiveId();
    setActive(active == null ? cycler().getFirst() : cycler().getNextAvailable(registry(), active));
  }

  /// Cycles to the previous available perspective in the cycle list.
  default void switchToPreviousAvailable() {
    var active = getActiveId();
    setActive(
        active == null ? cycler().getFirst() : cycler().getPreviousAvailable(registry(), active));
  }
}
