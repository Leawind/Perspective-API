package io.github.leawind.perspectiveapi.internal.impl.context;

import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import io.github.leawind.perspectiveapi.api.context.PerspectiveRenderTickContext;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;

public class PerspectiveRenderTickContextImpl implements PerspectiveRenderTickContext {

  private final PerspectiveManager manager;
  private float partialTicks;
  private Entity cameraEntity;
  private boolean isInTransition;

  public PerspectiveRenderTickContextImpl(PerspectiveManager manager) {
    this.manager = manager;
  }

  @Override
  public @NonNull PerspectiveManager manager() {
    return manager;
  }

  @Override
  public float partialTicks() {
    return partialTicks;
  }

  @Override
  public Entity entity() {
    return cameraEntity;
  }

  @Override
  public boolean isInTransition() {
    return isInTransition;
  }

  public void setup(float partialTicks, Entity cameraEntity, boolean isInTransition) {
    this.partialTicks = partialTicks;
    this.cameraEntity = cameraEntity;
    this.isInTransition = isInTransition;
  }
}
