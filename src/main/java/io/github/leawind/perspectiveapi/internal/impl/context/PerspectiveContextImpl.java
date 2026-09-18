package io.github.leawind.perspectiveapi.internal.impl.context;

import io.github.leawind.perspectiveapi.api.PerspectiveModifierContext;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import java.util.Objects;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

  /// Prepares the context for a frame. A null entity is only valid in entity-less contexts used
  /// by headless tests; production callers always pass the camera entity.
  public void setup(float partialTicks, @Nullable Entity cameraEntity, boolean isTransitioning) {
    this.partialTicks = partialTicks;
    this.cameraEntity = cameraEntity;
    this.isTransitioning = isTransitioning;
    perspectiveBaseState = null;
  }

  public void setPerspectiveBaseState(@NonNull PerspectiveState perspectiveBaseState) {
    this.perspectiveBaseState = Objects.requireNonNull(perspectiveBaseState);
  }
}
