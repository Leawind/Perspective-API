package io.github.leawind.perspectiveapi.platform.neoforge;

/*? if neoforge {*/
/*import com.google.auto.service.AutoService;
import io.github.leawind.perspectiveapi.platform.api.PlatformHelper;
import net.neoforged.fml.loading.FMLLoader;

@AutoService(PlatformHelper.class)
public class PlatformHelperImpl implements PlatformHelper {
  @Override
  public boolean isDevelopmentEnvironment() {
/^?   if >=1.21.11 {^/
    return !FMLLoader.getCurrent().isProduction();
/^?   } else {^/
/^return !FMLLoader.isProduction();
^//^?   }^/
  }
}
*//*?}*/
