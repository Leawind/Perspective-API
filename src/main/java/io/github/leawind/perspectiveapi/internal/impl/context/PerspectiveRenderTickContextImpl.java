package io.github.leawind.perspectiveapi.internal.impl.context;

import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import io.github.leawind.perspectiveapi.api.context.PerspectiveRenderTickContext;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;

public class PerspectiveRenderTickContextImpl implements PerspectiveRenderTickContext {

  private final PerspectiveManager manager;
  private float particalTicks;
  private Entity cameraEntity;

  public PerspectiveRenderTickContextImpl(PerspectiveManager manager) {
    this.manager = manager;
  }

  @Override
  public @NonNull PerspectiveManager manager() {
    return manager;
  }

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
