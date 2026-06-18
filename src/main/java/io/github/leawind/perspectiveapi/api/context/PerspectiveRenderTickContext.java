package io.github.leawind.perspectiveapi.api.context;

import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public interface PerspectiveRenderTickContext {
  float partialTicks();

  @Nullable Entity entity();
}
