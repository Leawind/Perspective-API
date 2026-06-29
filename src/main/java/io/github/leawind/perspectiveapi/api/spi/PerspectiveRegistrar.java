package io.github.leawind.perspectiveapi.api.spi;

import io.github.leawind.perspectiveapi.api.PerspectiveManager;

/// Service provider interface for registering custom perspectives.
///
/// Implementations are loaded via {@link java.util.ServiceLoader} during mod initialization.
public interface PerspectiveRegistrar {

  /// Registers perspectives with the manager.
  ///
  /// ### Example
  ///
  /// ```java
  /// manager.registry().register(ExamplePerspective.INSTANCE);
  /// ```
  ///
  /// @param manager the perspective manager to register with
  void register(PerspectiveManager manager);
}
