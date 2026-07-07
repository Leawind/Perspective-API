package io.github.leawind.perspectiveapi.api.spi;

import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;

/// Service provider interface for registering custom perspectives.
///
/// Implementations are loaded via {@link java.util.ServiceLoader} during mod initialization.
public interface PerspectiveRegistrar {

  /// ### Example
  ///
  /// ```java
  /// registry.register(ExamplePerspective.INSTANCE);
  /// ```
  void register(PerspectiveRegistry registry);
}
