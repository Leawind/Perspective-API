package io.github.leawind.perspectiveapi.platform.neoforge;

/*? if neoforge {*/
/*import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.logic.config.ConfigScreenManager;
import net.neoforged.fml.ModList;

public class NeoForgeModInitializer {
  static void initialize() {
    /^? if >=1.20.6 {^/
    ModList.get()
        .getModContainerById(PerspectiveAPI.MOD_ID)
        .ifPresent(
            container ->
                container.registerExtensionPoint(
                    net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                    (ignored, screen) -> ConfigScreenManager.findAndBuild(screen)));
    /^? } else {^/
    /^ModList.get()
        .getModContainerById(PerspectiveAPI.MOD_ID)
        .ifPresent(
            container ->
                container.registerExtensionPoint(
                    net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                    () ->
                        new net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, screen) -> ConfigScreenManager.findAndBuild(screen))));
    ^//^? }^/
  }
}
*//*? }*/
