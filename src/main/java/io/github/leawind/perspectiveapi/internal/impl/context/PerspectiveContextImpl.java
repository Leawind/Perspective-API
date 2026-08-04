package io.github.leawind.perspectiveapi.internal.impl.context;

import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
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
  public @NonNull Entity cameraEntity() {
    return Objects.requireNonNull(cameraEntity, "Perspective context is not initialized");
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
