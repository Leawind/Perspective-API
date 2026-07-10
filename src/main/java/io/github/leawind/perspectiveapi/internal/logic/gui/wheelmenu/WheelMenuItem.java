package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Represents a single perspective entry displayed in the wheel menu.
public final class WheelMenuItem {

  /// Availability status of a perspective in the wheel menu.
  public enum Availability {
    /// Not found in the global registry.
    UNREGISTERED,
    /// Registered but {@link Perspective#isAvailable()} returns `false`.
    UNAVAILABLE,
    /// Registered and available.
    AVAILABLE
  }

  private final @NonNull Identifier id;
  private @Nullable Perspective perspective;
  private @NonNull Availability availability;

  public WheelMenuItem(@NonNull Identifier id) {
    this.id = id;
    this.availability = Availability.UNREGISTERED;
  }

  public WheelMenuItem update(PerspectiveRegistry registry) {
    perspective = registry.get(id);

    if (perspective == null) {
      availability = Availability.UNREGISTERED;
    } else if (perspective.isAvailable()) {
      availability = Availability.AVAILABLE;
    } else {
      availability = Availability.UNAVAILABLE;
    }
    return this;
  }

  public @NonNull Component displayName() {
    if (perspective != null) return perspective.getNameComponent();
    return Component.literal(id.toString());
  }

  public @Nullable Component displayDescription() {
    if (perspective != null) return perspective.getDescriptionComponent();
    return null;
  }

  public @Nullable Identifier icon() {
    if (perspective != null) return perspective.icon();
    return null;
  }

  public @NonNull Identifier id() {
    return id;
  }

  public @Nullable Perspective perspective() {
    return perspective;
  }

  public @NonNull Availability availability() {
    return availability;
  }
}
