package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.spi.PerspectiveSwitcherBehavior;
import net.minecraft.client.CameraType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// A read-only view of a registered perspective.
///
/// Each instance is backed by a {@link PerspectiveBehavior} and carries metadata
/// derived from its {@link PerspectiveBehavior.Info} annotation. Perspectives are
/// created by the {@link PerspectiveRegistry} and should not be implemented directly.
///
/// @see PerspectiveBehavior
/// @see PerspectiveBehavior.Info
public interface Perspective {
  /// Returns the unique identifier of this perspective.
  @NonNull String id();

  /// Returns the display name of this perspective.
  @NonNull Component name();

  /// Returns the description of this perspective, or `null` if none.
  @Nullable Component description();

  /// Returns the vanilla camera type used as a fallback when this perspective
  /// does not explicitly modify the camera transform or FOV.
  @NonNull CameraType cameraType();

  /// Whether this perspective is allowed to be manually selected by the player
  /// via a {@link PerspectiveSwitcherBehavior}.
  boolean switchable();

  /// The sorting priority within the switcher.
  int priority();

  /// Returns the identifier of the icon texture for this perspective, or `null` if none.
  @Nullable Identifier icon();

  /// Returns whether this perspective is currently available to be selected or remain active.
  ///
  /// This delegates to the underlying {@link PerspectiveBehavior#isAvailable()}.
  boolean isAvailable();
}
