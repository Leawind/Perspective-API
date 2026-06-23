package io.github.leawind.perspectiveapi.platform.api;

/// Abstraction over platform-specific utilities (Fabric, Forge, NeoForge).
///
/// Each platform provides its own implementation of this interface.
public interface PlatformHelper {
  /// Returns {@code true} if the game is running in a development environment.
  boolean isDevelopmentEnvironment();
}
