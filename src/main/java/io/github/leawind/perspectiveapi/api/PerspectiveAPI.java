package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.state.StateManager;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;
import io.github.leawind.perspectiveapi.internal.impl.state.StateManagerImpl;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Entry point for the Perspective API mod.
public final class PerspectiveAPI {
  private PerspectiveAPI() {}

  public static final String MOD_ID = "perspective_api";
  public static final String MOD_NAME = "Perspective API";

  private static volatile boolean enabled = true;

  /// Master switch that controls whether all mixins and event handlers are active.
  /// Set to `false` to completely revert to vanilla behavior.
  ///
  /// @see #setEnabled(boolean)
  public static boolean isEnabled() {
    return enabled;
  }

  /// @see #isEnabled()
  public static void setEnabled(boolean enabled) {
    PerspectiveAPI.enabled = enabled;
  }

  /// @return The global singleton of {@link PerspectiveManager}.
  public static @NonNull PerspectiveManager getManager() {
    return PerspectiveManagerImpl.INSTANCE;
  }

  // region state manager

  private static volatile @Nullable StateManager stateManager = null;

  public static @NonNull StateManager getStateManager(@NonNull Minecraft minecraft) {
    var manager = stateManager;
    if (manager == null) {
      String fileName = PerspectiveAPI.MOD_ID + ".json";
      Path filePath = minecraft.gameDirectory.toPath().resolve("config").resolve(fileName);
      stateManager = manager = new StateManagerImpl(filePath);
    }
    return manager;
  }

  // endregion
}
