package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.spi.PerspectiveSwitcherBehavior;
import net.minecraft.client.CameraType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// @see PerspectiveBehavior.Meta
public interface Perspective {
  @NonNull String id();

  @NonNull Component name();

  @Nullable Component description();

  @NonNull CameraType cameraType();

  /// Whether this perspective is allowed to be manually selected by the player
  /// via a {@link PerspectiveSwitcherBehavior}.
  boolean switchable();

  /// The sorting priority within the switcher.
  int priority();

  @Nullable Identifier icon();

  /// Returns whether this perspective is currently available to be selected or remain active.
  ///
  /// This delegates to the underlying {@link PerspectiveBehavior#isAvailable()}.
  boolean isAvailable();
}
