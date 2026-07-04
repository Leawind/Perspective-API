package io.github.leawind.perspectiveapi.internal.logic.config;

import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class ConfigScreenManager {
  public interface ConfigScreenBuilder {
    @NonNull Screen build(Screen parent);
  }

  public static @NonNull Screen findAndBuild(Screen parent) {
    var builder = findBuilder();
    if (builder != null) {
      return builder.build(parent);
    }
    return buildFallbackScreen();
  }

  private static @Nullable ConfigScreenBuilder findBuilder() {
    if (isClassExists("dev.isxander.yacl3.api.YetAnotherConfigLib")) {
      return YaclConfigScreenBuilder::build;
    }
    return null;
  }

  private static Screen buildFallbackScreen() {
    return new ErrorScreen(
        text("config_screen.fallback.title"), text("config_screen.fallback.message"));
  }

  private static Component text(String key) {
    return Component.translatable(PerspectiveAPI.MOD_ID + "." + key);
  }

  private static boolean isClassExists(String className) {
    try {
      Class.forName(className, false, Thread.currentThread().getContextClassLoader());
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
