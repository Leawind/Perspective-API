package io.github.leawind.perspectiveapi.platform.neoforge;
/*? if neoforge {*/
/*import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.config.ConfigScreenManager;
import net.neoforged.fml.ModList;

public class NeoforgeModInitializer {
  static void initialize() {
    /^? if >=1.20.6 {^/
    ModList.get()
        .getModContainerById(PerspectiveAPI.MOD_ID)
        .ifPresent(
            container -> {
              var configScreenManager = ConfigScreenManager.getInstance();
              if (configScreenManager.hasBuilder()) {
                container.registerExtensionPoint(
                    net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                    (ignored, screen) -> configScreenManager.build(screen));
              }
            });
    /^? } else {^/
    /^ModList.get()
           .getModContainerById(PerspectiveAPI.MOD_ID)
           .ifPresent(
             container -> {
               var configScreenManager = ConfigScreenManager.getInstance();
               if (configScreenManager.hasBuilder()) {
                 container.registerExtensionPoint(
                   net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                   () ->
                     new net.neoforged.neoforge.client.ConfigScreenHandler.ConfigScreenFactory(
                       (minecraft, screen) -> configScreenManager.build(screen)));
               }
             });
    ^//^? }^/
  }
}
*//*? }*/
