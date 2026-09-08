package io.github.leawind.perspectiveapi.api;

import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Manages an ordered chain of perspective overrides.
///
/// Entries are evaluated in descending order of priority. The chain resolves to the first non-null
/// identifier whose perspective is registered and available. Invalid or unavailable candidates are
/// skipped. Entries with the same priority are evaluated in registration order. During each
/// main-camera render update, every visited supplier is evaluated at most once while Perspective
/// API is enabled.
@ApiStatus.NonExtendable
public interface PerspectiveOverrideChain {
  /// Registers an override and returns a handle that owns the registration.
  ///
  /// Override IDs are global, stable diagnostic identifiers used to attribute supplier failures in
  /// logs.
  ///
  /// ## Priority
  ///
  /// Higher priority values are evaluated first. Do not rely on registration order to coordinate
  /// equal-priority entries across mods.
  ///
  /// Choose a priority by comparing overrides that may be active at the same time.
  ///
  /// Search existing
  /// [usages](https://github.com/search?type=code&q="priority:perspective_api.override") first
  ///
  /// Leave gaps between priority values, such as increments of `1000`, so compatible overrides can
  /// be inserted later.
  ///
  /// @param id the non-empty stable override ID
  /// @param priority the evaluation priority
  /// @param supplier a supplier that returns the perspective ID, or `null` to skip It should return
  ///   a cached value when its state is updated on client ticks. A failure is logged and treated as
  ///   `null` for that render update.
  /// @throws IllegalArgumentException if `id` is empty or already registered
  @NonNull PerspectiveOverrideRegistration register(
      @NonNull String id, int priority, @NonNull Supplier<@Nullable String> supplier);
}
