package io.github.leawind.perspectiveapi.api;

import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Manages an ordered chain of perspective overrides.
///
/// Entries are evaluated in descending order of priority. The chain resolves to the first
/// non-null identifier that passes the provided validator.
public interface PerspectiveOverrideChain {

  /// Pushes an override entry into the chain.
  /// If an entry with the same key already exists, it is replaced.
  /// Higher priority values take precedence (are evaluated first).
  void push(
      @NonNull Identifier key, int priority, @NonNull Supplier<@Nullable Identifier> supplier);

  /// Removes the override entry with the given key.
  void pop(@NonNull Identifier key);

  /// Returns `true` if an override entry with the given key exists.
  boolean has(@NonNull Identifier key);

  /// Clears all entries from the chain.
  void clear();

  /// Clears all entries except those with the specified keys.
  void clearExcept(@NonNull Identifier... keys);

  /// Resolves the chain by evaluating suppliers in priority order.
  ///
  /// @param validator A predicate provided by the caller to determine if a generated identifier
  ///     is actually valid/available.
  /// @return The first valid identifier, or `null` if all entries fail or the chain is empty.
  @Nullable Identifier resolve(@NonNull Predicate<@NonNull Identifier> validator);
}
