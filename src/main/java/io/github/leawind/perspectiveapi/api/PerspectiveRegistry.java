package io.github.leawind.perspectiveapi.api;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Global singleton registry for querying {@link Perspective} instances.
///
/// Perspectives are registered during mod initialization
public interface PerspectiveRegistry {
  boolean contains(@Nullable String id);

  @NonNull List<String> getAll();

}
