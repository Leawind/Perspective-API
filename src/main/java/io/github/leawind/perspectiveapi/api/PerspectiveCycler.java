package io.github.leawind.perspectiveapi.api;

import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Manages an ordered list of perspective IDs for cycling via keybind.
///
/// Perspectives are ordered by priority. Lower priority values appear earlier in the cycle.
public interface PerspectiveCycler {
  /// Adds a perspective ID to the cycle list with the given priority. Replaces any existing entry
  /// with the same ID.
  @NonNull PerspectiveCycler add(@NonNull Identifier id, int priority);

  /// Removes a perspective ID from the cycle list. Does nothing if the ID is not present.
  void remove(@Nullable Identifier id);

  /// Removes all entries from the cycle list and resets the default perspective.
  void clear();

  /// @return a stream of all perspective IDs in priority order.
  @NonNull Stream<Identifier> stream();

  /// @return `true` if the cycle list is empty.
  boolean isEmpty();

  /// Returns the next cyclable perspective after `current`.
  @Nullable Identifier getNext(@Nullable Identifier current);

  /// @return the previous cyclable perspective before `current`.
  @Nullable Identifier getPrevious(@Nullable Identifier current);

  /// @return the first (lowest-priority) cyclable perspective, or `null` if the list is empty.
  @Nullable Identifier getFirst();

  /// @return the player's currently selected perspective ID in the cycle list, or `null` if
  /// none is selected.
  @Nullable Identifier getActive();

  /// Sets the player's selected perspective ID in the cycle list.
  void setActive(@Nullable Identifier id);

  /// Cycles to the next available perspective.
  void switchToNextAvailable(@NonNull PerspectiveRegistry registry);

  /// Cycles to the previous available perspective.
  void switchToPreviousAvailable(@NonNull PerspectiveRegistry registry);
}
