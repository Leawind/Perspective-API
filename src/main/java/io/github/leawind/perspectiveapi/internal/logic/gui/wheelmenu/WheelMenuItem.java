package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
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

  static class RenderState implements ExpSmooth.Value<RenderState> {
    public @Nullable Identifier icon = null;
    public boolean isSelected = false;
    /// Relative position to wheel center point
    public final Vector2f position = new Vector2f(0, 0);
    public float scale = 1.0f;
    public float alpha = 1.0f;

    public RenderState() {}

    @Override
    public RenderState set(@NonNull RenderState other) {
      icon = other.icon;
      isSelected = other.isSelected;
      position.set(other.position);
      scale = other.scale;
      alpha = other.alpha;
      return this;
    }

    @Override
    public RenderState lerp(RenderState target, float t, RenderState dest) {
      dest.icon = target.icon;
      dest.isSelected = target.isSelected;
      position.lerp(target.position, t, dest.position);
      dest.scale = scale + (target.scale - scale) * t;
      dest.alpha = alpha + (target.alpha - alpha) * t;
      return dest;
    }
  }

  private final ExpSmooth<RenderState> smoothRenderState =
      new ExpSmooth<>(RenderState::new).setHalflife(0.025);

  private final @NonNull Identifier id;
  private @Nullable Perspective perspective;
  private @NonNull Availability availability;

  public WheelMenuItem(@NonNull Identifier id) {
    this.id = id;
    this.availability = Availability.UNREGISTERED;
  }

  ExpSmooth<RenderState> getSmoothRenderState() {
    return smoothRenderState;
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
