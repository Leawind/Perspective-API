package io.github.leawind.perspectiveapi.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Global singleton registry for managing {@link Perspective} instances.
///
/// Perspectives are registered by their unique {@link Identifier} and can be
/// queried or activated.
///
/// Perspectives are expected to be registered during mod initialization.
public interface PerspectiveRegistry {

  /// Registers a perspective.
  ///
  /// Replace the perspective with the same id if it already exists.
  @NonNull PerspectiveRegistry register(@NonNull Perspective perspective)
      throws IllegalArgumentException;

  /// Removes the perspective with the given id.
  @Nullable Perspective unregister(@NonNull Identifier id);

  /// Removes all perspectives and resets active to `null`.
  void clear();

  /// Returns {@code true} if a perspective with the given id is registered.
  boolean contains(@Nullable Identifier id);

  /// Returns
  ///
  /// - the registered perspective with the given id, or
  /// - `null` if
  ///   - id not found
  ///   - id is null
  @Nullable Perspective get(@Nullable Identifier id);

  @NonNull List<Perspective> getAll();

  default boolean contains(@NonNull Perspective perspective) {
    return get(perspective.id()) == perspective;
  }
}
