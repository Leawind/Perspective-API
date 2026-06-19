package io.github.leawind.perspectiveapi.api;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface PerspectiveCycler {

  void set(@NonNull List<Identifier> list);

  @NonNull PerspectiveCycler add(@NonNull Identifier id, int priority);

  /// Removes a perspective id from the cycle list. Does nothing if the id is not present.
  void remove(@Nullable Identifier id);

  /// Removes all entries from the cycle list and resets the default perspective.
  void clear();

  @NonNull Stream<Identifier> stream();

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

  default @Nullable Identifier getNextAvailable(
      @NonNull PerspectiveRegistry registry, @NonNull Identifier current) {
    Identifier first = getFirst();
    if (first == null) return null;

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

  default @Nullable Identifier getPreviousAvailable(
      @NonNull PerspectiveRegistry registry, @NonNull Identifier current) {
    Identifier first = getFirst();
    if (first == null) return null;

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
