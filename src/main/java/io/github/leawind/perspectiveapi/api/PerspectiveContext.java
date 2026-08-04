package io.github.leawind.perspectiveapi.api;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Context provided during camera-state callbacks on the client thread.
///
/// @apiNote This object is valid only for the duration of the callback in which it is received. It
///   must not be stored or referenced after that callback returns.
@ApiStatus.NonExtendable
public interface PerspectiveContext {

  /// Returns the partial tick value for interpolation between ticks.
  float partialTicks();

  /// Returns the active camera entity used by vanilla camera setup for this frame.
  @NonNull Entity cameraEntity();

  /// Returns `true` if it is currently transitioning to this perspective.
  @ApiStatus.Experimental
  boolean isTransitioning();
}
