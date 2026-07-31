package io.github.leawind.perspectiveapi.internal.impl.context;

import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class PerspectiveContextImpl implements PerspectiveContext {

  private float partialTicks;
  private Entity cameraEntity;
  private boolean isTransitioning;

  public PerspectiveContextImpl() {}

  @Override
  public float partialTicks() {
    return partialTicks;
  }

  @Override
  public @Nullable Entity cameraEntity() {
    return cameraEntity;
  }

  @Override
  public boolean isTransitioning() {
    return isTransitioning;
  }

  public void setup(float partialTicks, @NonNull Entity cameraEntity, boolean isTransitioning) {
    this.partialTicks = partialTicks;
    this.cameraEntity = cameraEntity;
    this.isTransitioning = isTransitioning;
  }
}
