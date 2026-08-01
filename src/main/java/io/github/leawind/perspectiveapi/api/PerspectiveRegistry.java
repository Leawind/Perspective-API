package io.github.leawind.perspectiveapi.api;

import java.util.Collection;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Global singleton registry for {@link PerspectiveBehavior} instances.
@ApiStatus.NonExtendable
public interface PerspectiveRegistry {
  /// Registers a perspective.
  ///
  /// The behavior instance and perspective ID must both be unregistered. Runtime registration does
  /// not inspect annotations on the behavior class.
  ///
  /// @throws IllegalArgumentException if the ID or behavior instance is already registered
  @NonNull PerspectiveRegistration register(
      @NonNull PerspectiveInfo info, @NonNull PerspectiveBehavior behavior);

  /// Registers a runtime perspective that participates in default-perspective resolution.
  ///
  /// Higher default priorities take precedence. Ties are resolved by perspective ID.
  /// Runtime registration does not inspect annotations on the behavior class.
  ///
  /// @throws IllegalArgumentException if the ID or behavior instance is already registered
  @NonNull PerspectiveRegistration registerDefault(
      @NonNull PerspectiveInfo info, int defaultPriority, @NonNull PerspectiveBehavior behavior);

  /// Contributes semantic traits to the perspective with the given ID.
  ///
  /// Contributions are additive. They do not modify the target's {@link PerspectiveInfo}, cannot
  /// remove traits declared or contributed by another source, and are included in {@link
  /// Perspective#traits()} and {@link Perspective#hasTrait(String)}. No relationship or conflict
  /// between different trait names is inferred or checked.
  ///
  /// The target perspective does not need to be registered yet. The contribution remains associated
  /// with its stable ID if the target is removed and later registered again.
  ///
  /// @param contributorId the non-empty ID of the contributing mod or integration
  /// @param perspectiveId the non-empty exact ID of the target perspective
  /// @param traits the non-empty traits to contribute
  /// @throws IllegalArgumentException if an ID or the trait collection is empty, or a trait name is
  ///     invalid
  @ApiStatus.Experimental
  @NonNull PerspectiveTraitRegistration contributeTraits(
      @NonNull String contributorId,
      @NonNull String perspectiveId,
      @NonNull Collection<@NonNull String> traits);

  /// Checks if a perspective with the given ID is registered.
  ///
  /// @param id the perspective ID
  /// @return `true` if registered, `false` otherwise
  boolean contains(@Nullable String id);

  /// Retrieves a registered perspective by its ID.
  ///
  /// @param id the perspective ID
  /// @return the perspective, or `null` if not found
  @Nullable Perspective get(@NonNull String id);
}
