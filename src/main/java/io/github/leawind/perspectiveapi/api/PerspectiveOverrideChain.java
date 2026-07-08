package io.github.leawind.perspectiveapi.api;

import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

/// Manages an ordered chain of perspective overrides.
///
/// Entries are evaluated in descending order of priority. The chain resolves to the first
/// non-null identifier that passes the provided validator.
public interface PerspectiveOverrideChain extends Supplier<Identifier> {
  void push(@NonNull Identifier key, int priority, @NonNull Supplier<Identifier> supplier);

  /// Removes the override entry with the given key.
  void pop(@NonNull Identifier key);

  /// Returns `true` if an override entry with the given key exists.
  boolean has(@NonNull Identifier key);
}
