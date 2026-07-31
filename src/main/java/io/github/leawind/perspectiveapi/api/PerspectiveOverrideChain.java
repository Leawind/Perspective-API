package io.github.leawind.perspectiveapi.api;

import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Manages an ordered chain of perspective overrides.
///
/// Entries are evaluated in descending order of priority. The chain resolves to the first
/// non-null identifier whose perspective is registered and available. Invalid or unavailable
/// candidates are skipped. Entries with the same priority are evaluated in registration order.
/// Each supplier is evaluated once per current-perspective update while Perspective API is enabled.
@ApiStatus.NonExtendable
public interface PerspectiveOverrideChain {
  /// Registers an override and returns a handle that owns the registration.
  ///
  /// ## Priority
  ///
  /// Higher priority values are evaluated first.
  /// Do not rely on registration order to coordinate equal-priority entries across mods.
  ///
  /// Choose a priority by comparing overrides that may be active at the same time.
  ///
  /// Search existing
  /// [usages](https://github.com/search?type=code&q="priority:perspective_api.override")
  /// first
  ///
  /// Leave gaps between priority values, such as increments of `1000`, so compatible overrides can
  /// be inserted later.
  ///
  /// @param priority the evaluation priority
  /// @param supplier a supplier that returns the perspective ID, or `null` to skip
  ///   A failure is logged and treated as `null` for that update.
  @NonNull PerspectiveOverrideRegistration register(
      int priority, @NonNull Supplier<@Nullable String> supplier);
}
