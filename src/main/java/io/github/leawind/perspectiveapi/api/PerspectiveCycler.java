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
  @Nullable Identifier getNext(@Nullable Identifier current);

  /// Returns the previous cycable perspective before {@code current}.
  @Nullable Identifier getPrevious(@Nullable Identifier current);

  /// Returns the first (lowest-priority) cycable perspective, or {@code null} if the list is empty.
  @Nullable Identifier getFirst();

  /// Returns the player's currently selected perspective id in the cycle list, or {@code null} if
  /// none is selected.
  @Nullable Identifier getActive();

  /// Sets the player's selected perspective id in the cycle list.
  void setActive(@Nullable Identifier id);

  /// Cycles to the next available perspective
  void switchToNextAvailable(@NonNull PerspectiveRegistry registry);

  /// Cycles to the previous available perspective
  void switchToPreviousAvailable(@NonNull PerspectiveRegistry registry);
}
