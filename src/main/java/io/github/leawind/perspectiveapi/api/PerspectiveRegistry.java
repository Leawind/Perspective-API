package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Global singleton registry for {@link PerspectiveBehavior} instances.
@ApiStatus.NonExtendable
public interface PerspectiveRegistry {
  /// Registers a perspective.
  ///
  /// The behavior instance and perspective ID must both be unregistered. Runtime registration does
  /// not inspect annotations on the behavior class.
  ///
  /// @throws IllegalArgumentException if the ID or behavior instance is already registered
  @NonNull PerspectiveRegistration register(
      @NonNull PerspectiveInfo info, @NonNull PerspectiveBehavior behavior);

  /// Registers a runtime perspective that participates in default-perspective resolution.
  ///
  /// Higher default priorities take precedence. Ties are resolved by perspective ID. Runtime
  /// registration does not inspect annotations on the behavior class.
  ///
  /// @throws IllegalArgumentException if the ID or behavior instance is already registered
  @NonNull PerspectiveRegistration registerDefault(
      @NonNull PerspectiveInfo info, int defaultPriority, @NonNull PerspectiveBehavior behavior);

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
