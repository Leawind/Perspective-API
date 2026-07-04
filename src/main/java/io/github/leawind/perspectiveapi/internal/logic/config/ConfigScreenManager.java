package io.github.leawind.perspectiveapi.internal.logic.config;

import io.github.leawind.perspectiveapi.internal.utils.event.SimpleEventEmitter;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ConfigScreenManager {
  private static final ConfigScreenManager INSTANCE = new ConfigScreenManager();

  public static @NonNull ConfigScreenManager getInstance() {
    return INSTANCE;
  }

  private @Nullable Function<Screen, Screen> builder = null;

  public final SimpleEventEmitter.Owned<Void> onUpdate = SimpleEventEmitter.create();

  private ConfigScreenManager() {
    init();
  }

  public boolean hasBuilder() {
    return builder != null;
  }

  public @Nullable Screen buildOrNull(Screen parent) {
    var builder = this.builder;
    if (builder != null) {
      return builder.apply(parent);
    }
    return null;
  }

  public @NonNull Screen build(Screen parent) {
    return Objects.requireNonNull(builder).apply(parent);
  }

  private void init() {
    if (isClassExists("dev.isxander.yacl3.api.YetAnotherConfigLib")) {
      builder = parent -> new YaclConfigScreenBuilder().build(this, parent);
    }
  }

  private boolean isClassExists(String className) {
    try {
      ClassLoader.getSystemClassLoader().loadClass(className);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
