package io.github.leawind.perspectiveapi.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Global singleton registry for managing {@link Perspective} instances.
///
/// Perspectives are registered by their unique {@link Identifier} and can be
/// queried by id. The registry is add-only: once registered, a perspective
/// cannot be removed.
///
/// Perspectives are expected to be registered during mod initialization.
public interface PerspectiveRegistry {

  /// Registers a perspective.
  ///
  /// Replace the perspective with the same id if it already exists.
  @NonNull PerspectiveRegistry register(@NonNull Perspective perspective)
      throws IllegalArgumentException;

  /// Returns {@code true} if a perspective with the given id is registered.
  boolean contains(@Nullable Identifier id);

  /// Returns
  ///
  /// - the registered perspective with the given id, or
  /// - `null` if
  ///   - id not found
  ///   - id is null
  @Nullable Perspective get(@Nullable Identifier id);

  /// Returns all registered perspectives as an unmodifiable list.
  @NonNull List<Perspective> getAll();

  /// Returns {@code true} if the given perspective instance is registered.
  default boolean contains(@NonNull Perspective perspective) {
    return get(perspective.id()) == perspective;
  }
}
