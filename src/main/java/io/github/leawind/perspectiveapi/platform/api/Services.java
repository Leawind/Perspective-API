package io.github.leawind.perspectiveapi.platform.api;

import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Services {
  private static final Logger LOGGER = LoggerFactory.getLogger(Services.class);

  public static final PlatformHelper PLATFORM_HELPER = loadFirst(PlatformHelper.class);

  private static <T> T loadFirst(Class<T> clazz) {
    var loadedService =
        ServiceLoader.load(clazz)
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException("Failed to load service for " + clazz.getName()));

    LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
    return loadedService;
  }
}
