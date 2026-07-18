package io.github.leawind.perspectiveapi.api;

import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

/// Manages an ordered chain of perspective overrides.
///
/// Entries are evaluated in descending order of priority. The chain resolves to the first
/// non-null identifier that passes the provided validator.
@ApiStatus.NonExtendable
public interface PerspectiveOverrideChain {
  /// Pushes a new override entry to the chain.
  ///
  /// If an entry with the same key already exists, it is replaced.
  /// Higher priority values are evaluated first.
  ///
  /// @param key the unique identifier for this override entry
  /// @param priority the evaluation priority
  /// @param supplier a supplier that returns the perspective ID, or `null` to skip
  void push(@NonNull String key, int priority, @NonNull Supplier<String> supplier);

  /// Removes the override entry with the given key.
  void pop(@NonNull String key);

  /// Returns `true` if an override entry with the given key exists.
  boolean has(@NonNull String key);
}
