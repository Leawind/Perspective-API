package io.github.leawind.perspectiveapi.api;

import java.util.function.Supplier;
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

  /// Pushes an override entry into the override chain.
  ///
  /// If an entry with the same key already exists, it is replaced.
  /// Higher priority values take precedence (are evaluated first).
  void pushOverride(
      @NonNull Identifier key, int priority, @NonNull Supplier<@Nullable Identifier> supplier);

  /// Removes the override entry with the given key from the override chain.
  void popOverride(@NonNull Identifier key);

  /// Returns {@code true} if an override entry with the given key exists.
  boolean hasOverride(@NonNull Identifier key);

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
