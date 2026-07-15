package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Global singleton registry for {@link PerspectiveBehavior} instances.
public interface PerspectiveRegistry {
  boolean contains(@Nullable String id);

  @NonNull List<Perspective> getAll();

  @NonNull SimpleEventEmitter<Void> onUpdate();
}
