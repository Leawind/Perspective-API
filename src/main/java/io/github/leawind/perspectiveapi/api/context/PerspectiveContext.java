package io.github.leawind.perspectiveapi.api.context;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/// Context provided during perspective evaluation and lifecycle callbacks.
@ApiStatus.NonExtendable
public interface PerspectiveContext {

  /// Returns the partial tick value for interpolation between ticks.
  float partialTicks();

  /// Returns the camera entity, or `null` if unavailable.
  @Nullable Entity entity();

  /// Returns `true` if it is currently transitioning to this perspective.
  boolean isTransitioning();
}
