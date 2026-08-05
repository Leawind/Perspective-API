package io.github.leawind.perspectiveapi.internal.impl.context;

import io.github.leawind.perspectiveapi.api.PerspectiveModifierContext;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;

public class PerspectiveContextImpl implements PerspectiveModifierContext {

  private float partialTicks;
  private Entity cameraEntity;
  private boolean isTransitioning;
  private PerspectiveState perspectiveBaseState;

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

  @Override
  public @NonNull PerspectiveState perspectiveBaseState() {
    return Objects.requireNonNull(perspectiveBaseState, "Modifier context is not initialized");
  }

  public void setup(float partialTicks, @NonNull Entity cameraEntity, boolean isTransitioning) {
    this.partialTicks = partialTicks;
    this.cameraEntity = cameraEntity;
    this.isTransitioning = isTransitioning;
    perspectiveBaseState = null;
  }

  public void setPerspectiveBaseState(@NonNull PerspectiveState perspectiveBaseState) {
    this.perspectiveBaseState = Objects.requireNonNull(perspectiveBaseState);
  }
}
