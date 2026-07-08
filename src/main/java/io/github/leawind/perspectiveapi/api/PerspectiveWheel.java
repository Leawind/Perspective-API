package io.github.leawind.perspectiveapi.api;

import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/// Manages an ordered list of perspective IDs for cycling.
///
/// Perspectives are split into three categories:
/// - **selected**: user-ordered perspectives that appear first in the cycle
/// - **candidate**: registered but not user-ordered, sorted by priority
/// - **disabled**: excluded from the cycle
public interface PerspectiveWheel {

  @NonNull PerspectiveWheel register(@NonNull Identifier id, int priority);

  void unregister(@NonNull Identifier id);
}
