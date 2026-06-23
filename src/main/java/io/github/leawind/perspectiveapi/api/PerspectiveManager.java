package io.github.leawind.perspectiveapi.api;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/// Manages the lifecycle and state of camera perspectives.
///
/// Obtain via {@link PerspectiveAPI#getManager()}.
public interface PerspectiveManager {

  /// @return the perspective registry.
  @NonNull PerspectiveRegistry registry();

  /// @return the perspective cycler for cycling through perspectives.
  @NonNull PerspectiveCycler cycler();

  /// @return the override chain controller.
  @NonNull PerspectiveOverrideChain overrides();

  /// @return the transition controller.
  @NonNull Transition transition();

  @NonNull Identifier getDefault();

  /// Returns the current active perspective after resolving the override chain.
  /// Never returns `null`.
  @NonNull Perspective getCurrentPerspective();

  /// Returns the default perspective.
  default @NonNull Perspective getDefaultPerspective() {
    Perspective perspective = registry().get(getDefault());
    assert perspective != null;
    return perspective;
  }
}
