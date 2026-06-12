package io.github.leawind.perspectiveapi.api.context;

import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public interface PerspectiveTickContext {
  float partialTicks();

  @Nullable Entity entity();
}
