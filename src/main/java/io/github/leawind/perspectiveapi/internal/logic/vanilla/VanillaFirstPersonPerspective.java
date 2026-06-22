package io.github.leawind.perspectiveapi.internal.logic.vanilla;

import io.github.leawind.perspectiveapi.api.context.PerspectiveRenderTickContext;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import net.minecraft.client.CameraType;
import org.jspecify.annotations.NonNull;

public final class VanillaFirstPersonPerspective extends VanillaPerspective {
  public static final VanillaFirstPersonPerspective INSTANCE = new VanillaFirstPersonPerspective();

  private VanillaFirstPersonPerspective() {
    super("first_person", CameraType.FIRST_PERSON);
  }

  @Override
  public boolean shouldOverrideVanillaCamera() {
    return false;
  }

  @Override
  public void renderTick(@NonNull PerspectiveRenderTickContext context) {
    var entity = context.entity();
    if (entity == null) {
      return;
    }

    var pos = entity.getEyePosition(context.partialTicks());
    position.set(pos.x, pos.y, pos.z);

    PerspectiveUtils.getEntityRotation(entity, context.partialTicks(), rotation);
  }
}
