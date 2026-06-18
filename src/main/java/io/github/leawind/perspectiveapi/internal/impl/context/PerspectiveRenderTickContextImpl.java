package io.github.leawind.perspectiveapi.internal.impl.context;

import io.github.leawind.perspectiveapi.api.context.PerspectiveRenderTickContext;
import net.minecraft.world.entity.Entity;

public class PerspectiveRenderTickContextImpl implements PerspectiveRenderTickContext {

  private float particalTicks;
  private Entity cameraEntity;

  @Override
  public float partialTicks() {
    return particalTicks;
  }

  @Override
  public Entity entity() {
    return cameraEntity;
  }

  public void setup(float particalTicks, Entity cameraEntity) {
    this.particalTicks = particalTicks;
    this.cameraEntity = cameraEntity;
  }
}
