package io.github.leawind.perspectiveapi.internal.logic.builtin;

import io.github.leawind.perspectiveapi.api.context.PerspectiveRenderTickContext;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import net.minecraft.client.CameraType;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;

/// Built-in first-person perspective matching vanilla Minecraft behavior.
///
/// Does not override vanilla camera logic, allowing default first-person rendering.
public final class VanillaFirstPersonPerspective extends VanillaPerspective {
  /// Singleton instance of the first-person perspective.
  public static final VanillaFirstPersonPerspective INSTANCE = new VanillaFirstPersonPerspective();

  private final Vector3d tempPos = new Vector3d();
  private final Quaternionf tempRot = new Quaternionf();

  private VanillaFirstPersonPerspective() {
    super("first_person", CameraType.FIRST_PERSON);
  }

  @Override
  public void renderTick(@NonNull PerspectiveRenderTickContext context) {
    var entity = context.entity();
    if (entity == null) {
      return;
    }

    var pos = entity.getEyePosition(context.partialTicks());
    tempPos.set(pos.x, pos.y, pos.z);
    state.position.set(tempPos);
    state.hasPosition = true;

    PerspectiveUtils.getEntityRotation(entity, context.partialTicks(), tempRot);
    state.rotation.set(tempRot);
    state.hasRotation = true;
  }
}
