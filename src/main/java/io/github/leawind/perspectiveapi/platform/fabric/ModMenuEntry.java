package io.github.leawind.perspectiveapi.platform.fabric;

/*? if fabric {*/
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.leawind.perspectiveapi.internal.logic.config.ConfigScreenManager;

@SuppressWarnings("unused")
public final class ModMenuEntry implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return ConfigScreenManager::findAndBuild;
  }
}
/*?}*/
