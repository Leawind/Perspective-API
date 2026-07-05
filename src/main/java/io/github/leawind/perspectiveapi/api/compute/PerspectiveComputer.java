package io.github.leawind.perspectiveapi.api.compute;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public interface PerspectiveComputer {
  @Nullable Identifier computeId();
}
