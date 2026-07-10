package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

import io.github.leawind.perspectiveapi.api.Perspective;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Represents a single perspective entry displayed in the wheel menu.
public record WheelMenuItem(
    @NonNull Identifier id,
    @Nullable Perspective perspective,
    @NonNull Availability availability) {

  /// Availability status of a perspective in the wheel menu.
  enum Availability {
    /// Not found in the global registry.
    UNREGISTERED,
    /// Registered but {@link Perspective#isAvailable()} returns `false`.
    UNAVAILABLE,
    /// Registered and available.
    AVAILABLE
  }
}
