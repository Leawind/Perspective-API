package io.github.leawind.perspectiveapi.api;

import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Manages an ordered chain of perspective overrides.
///
/// Entries are evaluated in descending order of priority. The chain resolves to the first
/// non-null identifier whose perspective is registered and available. Invalid or unavailable
/// candidates are skipped. Entries with the same priority are evaluated in insertion order.
/// Replacing an entry counts as a new insertion for this ordering. Each supplier is evaluated once
/// per current-perspective update while Perspective API is enabled.
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface PerspectiveOverrideChain {
  /// Pushes a new override entry to the chain.
  ///
  /// If an entry with the same key already exists, it is replaced.
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
  /// Mark each registration with its purpose so others can find it:
  ///
  /// ```java
  /// // priority:perspective_api.override
  /// // Shows aerial view while teleporting, ahead of ordinary player overrides.
  /// overrides.register("examplemod:teleporting_view", 300000, ()->ID);
  /// ```
  ///
  /// @param key the unique identifier for this override entry
  /// @param priority the evaluation priority
  /// @param supplier a supplier that returns the perspective ID, or `null` to skip
  void register(@NonNull String key, int priority, @NonNull Supplier<@Nullable String> supplier);

  /// Removes the override entry with the given key.
  void unregister(@NonNull String key);

  /// Returns `true` if an override entry with the given key exists.
  boolean contains(@NonNull String key);
}
