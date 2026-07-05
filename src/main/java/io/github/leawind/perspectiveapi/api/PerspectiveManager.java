package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.compute.PerspectiveCycler;
import io.github.leawind.perspectiveapi.api.compute.PerspectiveOverrideChain;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/// Manages the lifecycle and state of camera perspectives.
///
/// Obtain via {@link PerspectiveAPI#getManager()}.
public interface PerspectiveManager {

  /// @return The transition controller.
  @NonNull TransitionController transition();

  /// @return The perspective registry.
  @NonNull PerspectiveRegistry registry();

  /// @return The override chain controller.
  @NonNull PerspectiveOverrideChain overrides();

  /// @return The perspective cycler for cycling through perspectives.
  @NonNull PerspectiveCycler cycler();

  /// Returns the current active perspective after resolving the override chain.
  /// Never returns `null`.
  @NonNull Perspective getCurrent();

  default boolean isCurrent(@NonNull Identifier id) {
    return getCurrent().id().equals(id);
  }

  default boolean isCurrent(@NonNull Perspective perspective) {
    return getCurrent() == perspective;
  }
}
