package io.github.leawind.perspectiveapi.api;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/// Manages the lifecycle and state of camera perspectives.
///
/// Obtain via {@link PerspectiveAPI#getManager()}.
public interface PerspectiveManager {

  /// @return the transition controller.
  @NonNull Transition transition();

  /// @return the perspective registry.
  @NonNull PerspectiveRegistry registry();

  /// @return the override chain controller.
  @NonNull PerspectiveOverrideChain overrides();

  /// @return the perspective cycler for cycling through perspectives.
  @NonNull PerspectiveCycler cycler();

  /// Returns the default perspective.
  @NonNull Perspective getDefault();

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
