package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Runtime metadata for a perspective.
///
/// {@link Declaration} is the declarative form used by service-loaded perspectives. This runtime
/// form uses resolved components and identifiers so it can describe perspectives created from
/// player data or other runtime sources.
///
/// @param id the non-empty stable perspective ID
/// @param name the display name
/// @param description the optional description
/// @param baseType the vanilla camera type used as a fallback
/// @param switchable whether player-facing switchers may select the perspective
/// @param priority the sorting priority within switchers, where lower values appear first
/// @param icon the optional icon texture
public record PerspectiveInfo(
    @NonNull String id,
    @NonNull Component name,
    @Nullable Component description,
    @NonNull BaseType baseType,
    boolean switchable,
    int priority,
    @Nullable Identifier icon) {

  public PerspectiveInfo {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(baseType);
    if (id.isEmpty()) throw new IllegalArgumentException("Perspective id must not be empty");
  }

  /// Creates runtime information from a declarative perspective annotation.
  public static @NonNull PerspectiveInfo fromDeclaration(@NonNull Declaration declaration) {
    Objects.requireNonNull(declaration);
    String id = declaration.id();
    Component name =
        Component.translatable(
            declaration.nameKey().isEmpty()
                ? "perspective." + id + ".name"
                : declaration.nameKey());
    Component description =
        declaration.descriptionKey().isEmpty()
            ? null
            : Component.translatable(declaration.descriptionKey());
    Identifier icon =
        declaration.icon().isEmpty() ? null : Bridge.parseIdentifier(declaration.icon());
    return new PerspectiveInfo(
        id,
        name,
        description,
        declaration.baseType(),
        declaration.switchable(),
        declaration.priority(),
        icon);
  }

  /// Creates a builder with the defaults used by {@link Declaration}.
  public static @NonNull Builder builder(@NonNull String id, @NonNull Component name) {
    return new Builder(id, name);
  }

  /// Builds runtime perspective information using annotation-compatible defaults.
  public static final class Builder {
    private final String id;
    private final Component name;
    private @Nullable Component description;
    private BaseType baseType = BaseType.THIRD_PERSON_BACK;
    private boolean switchable = true;
    private int priority;
    private @Nullable Identifier icon;

    private Builder(@NonNull String id, @NonNull Component name) {
      this.id = Objects.requireNonNull(id);
      this.name = Objects.requireNonNull(name);
    }

    public @NonNull Builder description(@Nullable Component description) {
      this.description = description;
      return this;
    }

    public @NonNull Builder baseType(@NonNull BaseType baseType) {
      this.baseType = Objects.requireNonNull(baseType);
      return this;
    }

    public @NonNull Builder switchable(boolean switchable) {
      this.switchable = switchable;
      return this;
    }

    public @NonNull Builder priority(int priority) {
      this.priority = priority;
      return this;
    }

    public @NonNull Builder icon(@Nullable Identifier icon) {
      this.icon = icon;
      return this;
    }

    public @NonNull PerspectiveInfo build() {
      return new PerspectiveInfo(id, name, description, baseType, switchable, priority, icon);
    }
  }

  /// Declarative metadata for a service-loaded perspective behavior.
  ///
  /// Every {@link java.util.ServiceLoader service-loaded} {@link PerspectiveBehavior} must carry
  /// this annotation. Runtime registrations use {@link PerspectiveInfo} directly.
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Documented
  public @interface Declaration {
    /// The non-empty identifier for this perspective.
    ///
    /// Recommended format: `<modid>.<path>` (e.g., `examplemod.free_camera`).
    /// If multiple behaviors use the same ID, the behavior with the lower {@link #priority()}
    /// value is registered. Ties are resolved by the lexicographically earlier fully qualified
    /// behavior class name.
    @NonNull String id();

    /// The vanilla camera type used as a fallback when this perspective does not explicitly modify
    /// the camera transform or projection settings.
    @NonNull BaseType baseType() default BaseType.THIRD_PERSON_BACK;

    /// The translation key for the perspective's display name.
    ///
    /// If left empty, it defaults to `perspective.<id>.name`.
    ///
    /// @see Perspective#name()
    @NonNull String nameKey() default "";

    /// The translation key for the perspective's description.
    ///
    /// If left empty, it defaults to `null`.
    ///
    /// @see Perspective#description()
    @NonNull String descriptionKey() default "";

    /// The string representation of the `Identifier` or `ResourceLocation` for the perspective's
    /// icon texture.
    ///
    /// If left empty, it defaults to `null`.
    ///
    /// @see Perspective#icon()
    @NonNull String icon() default "";

    /// Whether this perspective is allowed to be manually selected by the player via a {@link
    /// PerspectiveSwitcherBehavior}.
    ///
    /// If set to `false`, the perspective can only be activated programmatically through the
    /// {@link PerspectiveOverrideChain}.
    @ApiStatus.Experimental
    boolean switchable() default true;

    /// The sorting priority within the switcher and duplicate-ID resolution.
    ///
    /// Lower values appear earlier in the cycle and take precedence over a duplicate ID.
    /// Switcher ordering is effective only when {@link #switchable()} is `true`, but duplicate-ID
    /// resolution always uses this value.
    int priority() default 0;
  }

  /// Marks a service-loaded perspective behavior as a default perspective.
  ///
  /// Runtime registrations use {@link PerspectiveRegistry#registerDefault} instead.
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Documented
  public @interface Default {
    /// The priority of this default perspective. Higher values take precedence if multiple
    /// defaults are registered.
    int priority() default 0;
  }
}
