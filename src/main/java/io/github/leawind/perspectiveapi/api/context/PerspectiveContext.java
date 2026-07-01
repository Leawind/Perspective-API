package io.github.leawind.perspectiveapi.api.context;

import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Context provided during perspective evaluation and lifecycle callbacks.
public interface PerspectiveContext {

  /// Returns the perspective manager.
  @NonNull PerspectiveManager manager();

  /// Returns the partial tick value for interpolation between ticks.
  float partialTicks();

  /// Returns the camera entity, or `null` if unavailable.
  @Nullable Entity entity();

  /// Returns `true` if it is currently transitioning to this perspective.
  boolean isTransitioning();
}
