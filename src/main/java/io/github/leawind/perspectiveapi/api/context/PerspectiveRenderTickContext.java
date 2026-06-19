package io.github.leawind.perspectiveapi.api.context;

import io.github.leawind.perspectiveapi.api.PerspectiveManager;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

public interface PerspectiveRenderTickContext {
  PerspectiveManager manager();
  
  float partialTicks();

  @Nullable Entity entity();
}
