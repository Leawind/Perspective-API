package io.github.leawind.perspectiveapi.platform.api;

import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;

/// Abstraction over platform-specific utilities (Fabric, Forge, NeoForge).
///
/// Each platform provides its own implementation of this interface.
public interface PlatformHelper {

  /// Returns the current Minecraft data version number.
  default int getDataVersion() {
    /*? if >=1.21.11 {*/
    return SharedConstants.getCurrentVersion().dataVersion().version();
    /*? } else {*/
    /*return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
     
    *//*? }*/
  }

  /// Creates an {@link Identifier} using the default {@code minecraft} namespace.
  default Identifier createIdentifier(String path) {
    return createIdentifier("minecraft", path);
  }

  default Identifier createIdentifier(String namespace, String path) {
    /*? if >1.20.4 {*/
    return Identifier.fromNamespaceAndPath(namespace, path);
    /*? } else {*/
    /*return new Identifier(namespace, path);
    *//*? }*/
  }

  /// Returns {@code true} if the game is running in a development environment.
  boolean isDevelopmentEnvironment();
}
