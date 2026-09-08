package io.github.leawind.perspectiveapi.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
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
/// @param order the sorting order within the built-in perspective switcher, where lower values
///   appear first
/// @param icon the optional icon texture
/// @param traits the stable traits declared when the perspective is registered; see {@link
///   Declaration#traits()} for recommended shared traits
public record PerspectiveInfo(
    @NonNull String id,
    @NonNull Component name,
    @Nullable Component description,
    int order,
    @Nullable Identifier icon,
    @ApiStatus.Experimental @NonNull Set<@NonNull String> traits) {

  private static final Pattern TRAIT_PATTERN =
      Pattern.compile("(?:[a-z0-9_.-]+:)?[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

  public PerspectiveInfo {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(traits);
    if (id.isEmpty()) throw new IllegalArgumentException("Perspective id must not be empty");
    traits.forEach(PerspectiveInfo::validateTrait);
    traits = Set.copyOf(traits);
  }

  /// Returns whether this metadata declares the given trait.
  ///
  /// Trait names use lowercase `snake_case`, such as `third_person`. Shared traits should remain
  /// unqualified so independent mods can agree on the same meaning. A mod-specific trait may use
  /// the form `<namespace>:<trait>` until a shared meaning is established.
  @ApiStatus.Experimental
  public boolean hasTrait(@NonNull String trait) {
    validateTrait(trait);
    return traits.contains(trait);
  }

  /// Validates a trait name.
  ///
  /// @throws NullPointerException if `trait` is `null`
  /// @throws IllegalArgumentException if `trait` is not lowercase `snake_case` with an optional
  ///   namespace
  @ApiStatus.Experimental
  private static void validateTrait(@NonNull String trait) {
    Objects.requireNonNull(trait);
    if (!TRAIT_PATTERN.matcher(trait).matches()) {
      throw new IllegalArgumentException("Invalid perspective trait: '" + trait + "'");
    }
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
    private int order;
    private @Nullable Identifier icon;
    private final Set<String> traits = new LinkedHashSet<>();

    private Builder(@NonNull String id, @NonNull Component name) {
      this.id = Objects.requireNonNull(id);
      this.name = Objects.requireNonNull(name);
    }

    public @NonNull Builder description(@Nullable Component description) {
      this.description = description;
      return this;
    }

    public @NonNull Builder order(int order) {
      this.order = order;
      return this;
    }

    public @NonNull Builder icon(@Nullable Identifier icon) {
      this.icon = icon;
      return this;
    }

    /// Adds a trait.
    ///
    /// Adding the same trait more than once has no additional effect.
    @ApiStatus.Experimental
    public @NonNull Builder trait(@NonNull String trait) {
      validateTrait(trait);
      traits.add(trait);
      return this;
    }

    /// Adds traits.
    @ApiStatus.Experimental
    public @NonNull Builder traits(@NonNull Collection<@NonNull String> traits) {
      Objects.requireNonNull(traits);
      traits.forEach(this::trait);
      return this;
    }

    public @NonNull PerspectiveInfo build() {
      return new PerspectiveInfo(id, name, description, order, icon, traits);
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
    /// If multiple behaviors use the same ID, the behavior with the higher {@link #precedence()}
    /// value is registered. Ties are resolved by the lexicographically earlier fully qualified
    /// behavior class name.
    ///
    /// ### Recommended Format
    ///
    /// ```
    /// <modid>.<path>
    /// ```
    ///
    /// ### Example
    ///
    /// - `examplemod.free_camera`
    @NonNull String id();

    /// The translation key for the perspective's display name.
    ///
    /// If left empty, it defaults to `perspective.<id>.name`, where `<id>` is {@link #id()}
    ///
    /// ### Recommended Format
    ///
    /// ```
    /// perspective.<id>.name
    /// ```
    ///
    /// @see PerspectiveInfo#name()
    @NonNull String nameKey() default "";

    /// The translation key for the perspective's description.
    ///
    /// If left empty, it defaults to `null`.
    ///
    /// ### Recommended Format
    ///
    /// ```
    /// perspective.<id>.description
    /// ```
    ///
    /// @see PerspectiveInfo#description()
    @NonNull String descriptionKey() default "";

    /// The string representation of the `Identifier` for the perspective's icon texture.
    ///
    /// If left empty, it defaults to `null` which means no icon.
    ///
    /// ### Recommended Format
    ///
    /// ```
    /// <namespace>:textures/perspective/<path>.png
    /// ```
    ///
    /// @see PerspectiveInfo#icon()
    @NonNull String icon() default "";

    /// The sorting order within the built-in perspective switcher.
    ///
    /// Lower values appear earlier in the selector. Selector ordering is effective only when the
    /// `switchable` trait is declared. This value does not resolve duplicate IDs; see {@link
    /// #precedence()}.
    int order() default 0;

    /// The precedence that resolves duplicate service-discovered IDs.
    ///
    /// When multiple behaviors declare the same {@link #id()}, the behavior with the higher value
    /// is registered. Ties are resolved by the lexicographically earlier fully qualified behavior
    /// class name. Runtime registrations reject duplicate IDs instead, so they never use this
    /// value. This value does not affect display order.
    int precedence() default 0;

    /// Stable traits of this perspective.
    ///
    /// Traits form an open vocabulary. Unknown traits remain valid, and consumers should test only
    /// traits whose documented meaning they understand. Recommended shared traits include:
    ///
    /// - `switchable`: the perspective is suitable for general-purpose player-facing perspective
    ///   switchers
    /// - `first_person`: the perspective primarily observes from the camera entity's eyes
    /// - `third_person`: the perspective primarily observes the camera entity from outside
    /// - `controllable`: standard mouse-look input continuously and predictably controls the
    ///   visible viewing direction
    ///
    /// Traits describe stable perspective characteristics and integration hints, not transient
    /// per-frame state. They cannot be changed after the perspective is registered. Shared traits
    /// should use lowercase `snake_case` without a namespace. Mod-specific traits may use
    /// `<namespace>:<trait>`.
    ///
    /// `controllable` should be declared only when adjusted standard mouse-look deltas are consumed
    /// by the perspective's control implementation and feedback from target screen position can
    /// form a stable closed loop. It does not promise that the client is currently capturing mouse
    /// input or grant exclusive ownership of that input.
    ///
    /// The built-in perspective switcher includes only perspectives that declare `switchable`. This
    /// trait does not prevent a perspective from being selected directly through {@link
    /// PerspectiveSelection}, a {@link PerspectiveOverrideChain}, or a dedicated user interface.
    @ApiStatus.Experimental
    @NonNull String[] traits() default {};
  }

  /// Marks a service-loaded perspective behavior as a default perspective.
  ///
  /// Runtime registrations use {@link PerspectiveRegistry#registerDefault} instead.
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Documented
  public @interface Default {
    /// The priority of this default perspective. Higher values take precedence if multiple defaults
    /// are registered.
    int priority() default 0;
  }
}
