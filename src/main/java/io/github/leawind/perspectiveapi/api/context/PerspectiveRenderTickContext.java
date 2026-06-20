package io.github.leawind.perspectiveapi.api.context;

import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Context provided during a render tick callback while a perspective is active.
public interface PerspectiveRenderTickContext {

  /// Returns the perspective manager.
  @NonNull PerspectiveManager manager();

  /// Returns the partial tick value for interpolation between ticks.
  float partialTicks();

  /// Returns the camera entity, or `null` if unavailable.
  @Nullable Entity entity();

  /// Returns `true` if the it is currently transitioning to this perspective.
  boolean isInTransition();
}
