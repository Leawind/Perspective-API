package io.github.leawind.perspectiveapi.spi;

import java.util.ServiceLoader;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PerspectiveSPI {
  public static Stream<PerspectiveRegistrar> load() {
    ServiceLoader<PerspectiveRegistrar> loader = ServiceLoader.load(PerspectiveRegistrar.class);
    return StreamSupport.stream(loader.spliterator(), false);
  }
}
