package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class Factory {
  public static PerspectiveManager getPerspectiveManager() {
    return PerspectiveManagerImpl.INSTANCE;
  }
}
