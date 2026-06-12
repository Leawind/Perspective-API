package io.github.leawind.perspectiveapi.spi;

import io.github.leawind.perspectiveapi.api.PerspectiveManager;

public interface PerspectiveRegistrar {
  void register(PerspectiveManager manager);
}
