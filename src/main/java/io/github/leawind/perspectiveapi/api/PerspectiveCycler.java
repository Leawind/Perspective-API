package io.github.leawind.perspectiveapi.api;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Manages an ordered list of perspective ids for cycling via keybind
///
/// Perspectives are ordered by priority. Lower priority values appear earlier in the cycle.
public interface PerspectiveCycler {

  /// Replaces the entire cycle list with the given ids, assigned priorities by their index order.
  void set(@NonNull List<Identifier> list);

  /// Adds a perspective id to the cycle list with the given priority. Replaces any existing entry
  /// with the same id.
  @NonNull PerspectiveCycler add(@NonNull Identifier id, int priority);

  /// Removes a perspective id from the cycle list. Does nothing if the id is not present.
  void remove(@Nullable Identifier id);

  /// Removes all entries from the cycle list and resets the default perspective.
  void clear();

  /// Returns a stream of all perspective ids in priority order.
  @NonNull Stream<Identifier> stream();

  /// Returns {@code true} if the cycle list is empty.
  boolean isEmpty();

  /// Returns the next cycable perspective after {@code current}.
  ///
  /// Looks up the perspective in {@link PerspectiveRegistry}; skips non-cycable entries.
  /// If {@code current} is not in the list, returns the first entry.
  @Nullable Identifier getNext(@Nullable Identifier current);

  /// Returns the previous cycable perspective before {@code current}.
  ///
  /// Looks up the perspective in {@link PerspectiveRegistry}; skips non-cycable entries.
  /// If {@code current} is not in the list, returns the last entry.
  @Nullable Identifier getPrevious(@Nullable Identifier current);

  /// Returns the first (lowest-priority) cycable perspective, or {@code null} if the list is empty.
  @Nullable Identifier getFirst();

  /// Returns the next available (registered) perspective after `current`, wrapping around the
  /// cycle.
  /// Returns `null` if the cycle is empty or no available perspective is found.
  default @Nullable Identifier getNextAvailable(
      @NonNull PerspectiveRegistry registry, @NonNull Identifier current) {
    if (isEmpty()) return null;

    Identifier next = current;
    do {
      next = getNext(next);
      var perspective = registry.get(next);
      if (perspective != null) {
        return next;
      }
    } while (next != current);
    return null;
  }

  /// Returns the previous available (registered) perspective before `current`, wrapping around the
  /// cycle.
  /// Returns `null` if the cycle is empty or no registered perspective is found.
  default @Nullable Identifier getPreviousAvailable(
      @NonNull PerspectiveRegistry registry, @NonNull Identifier current) {
    if (isEmpty()) return null;

    Identifier previous = current;
    do {
      previous = getPrevious(previous);
      var perspective = registry.get(previous);
      if (perspective != null) {
        return previous;
      }
    } while (previous != current);
    return null;
  }
}
