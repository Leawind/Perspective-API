package io.github.leawind.perspectiveapi.api;

import net.minecraft.client.CameraType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// @see PerspectiveBehavior.Meta
public interface PerspectiveMeta {
  @NonNull String id();

  @Nullable Component name();

  @Nullable Component description();

  @NonNull CameraType cameraType();

  /// Whether this perspective is allowed to be manually selected by the player
  /// via a {@link io.github.leawind.perspectiveapi.api.spi.PerspectiveSwitcher}.
  boolean switchable();

  /// The sorting priority within the switcher.
  int priority();

  @Nullable Identifier icon();
}
