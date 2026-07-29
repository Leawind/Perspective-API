package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Global singleton registry for {@link PerspectiveBehavior} instances.
@ApiStatus.NonExtendable
public interface PerspectiveRegistry {
  /// Checks if a perspective with the given ID is registered.
  ///
  /// @param id the perspective ID
  /// @return `true` if registered, `false` otherwise
  boolean contains(@Nullable String id);

  /// Retrieves a registered perspective by its ID.
  ///
  /// @param id the perspective ID
  /// @return the perspective, or `null` if not found
  @Nullable Perspective get(@NonNull String id);
}
