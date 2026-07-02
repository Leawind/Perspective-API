package io.github.leawind.perspectiveapi.platform.forge;

/*? if forge {*/
/*import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyFieldOfViewContext;
import io.github.leawind.perspectiveapi.internal.logic.ModEntrypoint;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(PerspectiveAPI.MOD_ID)
public class Entrypoint {
  public Entrypoint(final FMLJavaModLoadingContext context) {
    if (FMLEnvironment.dist != Dist.CLIENT) {
      return;
    }
    ModEntrypoint.initialize();
    initialize();
  }

  private static final ModifyFieldOfViewContext modifyFovContext = new ModifyFieldOfViewContext();

  private static void initialize() {}

  @Mod.EventBusSubscriber(modid = PerspectiveAPI.MOD_ID)
  public static class ModEventHandler {

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov computeFovEvent) {
      modifyFovContext.setup((float) computeFovEvent.getFOV());
      GameClientEvents.MODIFY_FIELD_OF_VIEW.emit(modifyFovContext);
      computeFovEvent.setFOV(modifyFovContext.fieldOfView);
    }
  }
}
*//*?}*/
