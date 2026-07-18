package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Global singleton registry for {@link PerspectiveBehavior} instances.
public interface PerspectiveRegistry {
  boolean contains(@Nullable String id);

  @Nullable Perspective get(@NonNull String id);

  @ApiStatus.Experimental
  @NonNull List<@NonNull Perspective> getAll();

  @ApiStatus.Experimental
  @NonNull SimpleEventEmitter<Void> onUpdate();
}
