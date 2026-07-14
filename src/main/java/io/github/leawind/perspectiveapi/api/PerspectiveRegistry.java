package io.github.leawind.perspectiveapi.api;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Global singleton registry for managing {@link Perspective} instances.
///
/// The registry is the sole owners of all registered Perspective instances. Other components should
/// obtain Perspective references through this registry (via {@link #get} or {@link #getAll}) rather
/// than holding direct references.
///
/// Perspectives are registered by their unique ID and can be
/// queried by ID. The registry is add-only: once registered, a perspective
/// cannot be removed.
///
/// Perspectives are expected to be registered during mod initialization.
public interface PerspectiveRegistry {
  /// Registers a perspective.
  ///
  /// Replaces the perspective with the same ID if it already exists.
  @NonNull PerspectiveRegistry register(@NonNull Perspective perspective);

  /// Returns `true` if a perspective with the given ID is registered.
  boolean contains(@Nullable String id);

  /// ### Returns
  ///
  /// - the registered perspective with the given ID, or
  /// - `null` if:
  ///   - ID not found
  ///   - ID is null
  @Nullable Perspective get(@Nullable String id);

  @NonNull Perspective getDefault();

  @Nullable Perspective getOrDefault(@Nullable String id);

  /// Returns all registered perspectives as an unmodifiable list.
  @NonNull List<Perspective> getAll();
}
