package io.github.leawind.perspectiveapi.api;

import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
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
/// @param baseType the opaque vanilla camera type selected while the perspective is active
/// @param switchable whether player-facing switchers may select the perspective
/// @param priority the sorting priority within switchers, where lower values appear first
/// @param icon the optional icon texture
/// @param traits the stable semantic traits declared when the perspective is registered; see {@link
///   Declaration#traits()} for recommended shared traits
public record PerspectiveInfo(
    @NonNull String id,
    @NonNull Component name,
    @Nullable Component description,
    @NonNull BaseType baseType,
    @ApiStatus.Experimental boolean switchable,
    int priority,
    @Nullable Identifier icon,
    @ApiStatus.Experimental @NonNull Set<@NonNull String> traits) {

  private static final Pattern TRAIT_PATTERN =
      Pattern.compile("(?:[a-z0-9_.-]+:)?[a-z][a-z0-9]*(?:_[a-z0-9]+)*");

  public PerspectiveInfo {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(baseType);
    Objects.requireNonNull(traits);
    if (id.isEmpty()) throw new IllegalArgumentException("Perspective id must not be empty");
    traits.forEach(PerspectiveInfo::validateTrait);
    traits = Set.copyOf(traits);
  }

  /// Returns whether this metadata declares the given semantic trait.
  ///
  /// Trait names use lowercase `snake_case`, such as `third_person`. Shared traits should remain
  /// unqualified so independent mods can agree on the same meaning. A mod-specific trait may use
  /// the form `<namespace>:<trait>` until a shared meaning is established.
  @ApiStatus.Experimental
  public boolean hasTrait(@NonNull String trait) {
    validateTrait(trait);
    return traits.contains(trait);
  }

  /// Validates a semantic trait name.
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
    private BaseType baseType = BaseType.THIRD_PERSON_BACK;
    private boolean switchable = true;
    private int priority;
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

    /// Adds a semantic trait.
    ///
    /// Adding the same trait more than once has no additional effect.
    @ApiStatus.Experimental
    public @NonNull Builder trait(@NonNull String trait) {
      validateTrait(trait);
      traits.add(trait);
      return this;
    }

    /// Adds semantic traits.
    @ApiStatus.Experimental
    public @NonNull Builder traits(@NonNull Collection<@NonNull String> traits) {
      Objects.requireNonNull(traits);
      traits.forEach(this::trait);
      return this;
    }

    public @NonNull PerspectiveInfo build() {
      return new PerspectiveInfo(
          id, name, description, baseType, switchable, priority, icon, traits);
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
    /// Recommended format: `<modid>.<path>` (e.g., `examplemod.free_camera`). If multiple behaviors
    /// use the same ID, the behavior with the lower {@link #priority()} value is registered. Ties
    /// are resolved by the lexicographically earlier fully qualified behavior class name.
    @NonNull String id();

    /// The opaque vanilla camera type selected while this perspective is active.
    ///
    /// Minecraft uses its current camera type for version-dependent behavior beyond camera position
    /// and rotation. This value maps to that enum exactly; it does not describe or imply stable
    /// Perspective API semantics.
    @NonNull BaseType baseType() default BaseType.THIRD_PERSON_BACK;

    /// The translation key for the perspective's display name.
    ///
    /// If left empty, it defaults to `perspective.<id>.name`.
    ///
    /// @see PerspectiveInfo#name()
    @NonNull String nameKey() default "";

    /// The translation key for the perspective's description.
    ///
    /// If left empty, it defaults to `null`.
    ///
    /// @see PerspectiveInfo#description()
    @NonNull String descriptionKey() default "";

    /// The string representation of the `Identifier` for the perspective's icon texture.
    ///
    /// If left empty, it defaults to `null`.
    ///
    /// @see PerspectiveInfo#icon()
    @NonNull String icon() default "";

    /// Whether this perspective is allowed to be manually selected by the player via a {@link
    /// PerspectiveSwitcherBehavior}.
    ///
    /// If set to `false`, the perspective can only be activated programmatically through the {@link
    /// PerspectiveOverrideChain}.
    @ApiStatus.Experimental
    boolean switchable() default true;

    /// The sorting priority within the switcher and duplicate-ID resolution.
    ///
    /// Lower values appear earlier in the cycle and take precedence over a duplicate ID. Switcher
    /// ordering is effective only when {@link #switchable()} is `true`, but duplicate-ID resolution
    /// always uses this value.
    int priority() default 0;

    /// Semantic traits of this perspective.
    ///
    /// Traits form an open vocabulary. Unknown traits remain valid, and consumers should test only
    /// traits whose documented meaning they understand. Recommended shared traits include:
    ///
    /// - `first_person`: the perspective primarily observes from the camera entity's eyes
    /// - `third_person`: the perspective primarily observes the camera entity from outside
    /// - `controllable`: standard mouse-look input continuously and predictably controls the
    ///   visible viewing direction
    ///
    /// Traits describe stable perspective semantics, not transient per-frame state. They cannot be
    /// changed after the perspective is registered. Shared traits should use lowercase `snake_case`
    /// without a namespace. Mod-specific traits may use `<namespace>:<trait>`.
    ///
    /// `controllable` should be declared only when adjusted standard mouse-look deltas are consumed
    /// by the perspective's control implementation and feedback from target screen position can
    /// form a stable closed loop. It does not promise that the client is currently capturing mouse
    /// input or grant exclusive ownership of that input.
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
