package io.github.leawind.perspectiveapi.api;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Manages an ordered list of perspective IDs for cycling via keybind.
///
/// Perspectives are ordered by priority. Lower priority values appear earlier in the cycle.
public interface PerspectiveCycler extends Supplier<Identifier> {

  /// Adds a perspective ID to the cycle list with the given priority. Replaces any existing entry
  /// with the same ID.
  @NonNull PerspectiveCycler add(@NonNull Identifier id, int priority);

  /// Removes a perspective ID from the cycle list. Does nothing if the ID is not present.
  void remove(@Nullable Identifier id);

  /// Removes all entries from the cycle list and resets the default perspective.
  void clear();

  /// Returns the custom order list.
  ///
  /// The returned list is unmodifiable and always contains all IDs in the cycler.
  /// If {@link #setCustomOrder(List)} has never been called, returns IDs sorted by priority.
  /// If {@link #setCustomOrder(List)} was called, returns the order defined by it,
  /// with any missing IDs appended sorted by priority.
  ///
  /// @return an unmodifiable list of all IDs in custom order
  @NonNull List<@NonNull Identifier> getCustomOrder();

  /// Returns whether custom order is enabled.
  ///
  /// When enabled, {@link #getIds()} returns IDs in the order set by
  /// {@link #setCustomOrder(List)}, with any remaining IDs appended in priority order.
  boolean isCustomOrderEnabled();

  /// Enables or disables custom order.
  ///
  /// @see #setCustomOrder(List)
  /// @see #isCustomOrderEnabled()
  void setCustomOrderEnabled(boolean enabled);

  /// Sets the custom order for the cycle list.
  ///
  /// This method does not change the value of {@link #isCustomOrderEnabled()}.
  ///
  /// The given list defines the leading portion of the order. IDs listed here appear
  /// first in {@link #getCustomOrder()} and {@link #getIds()}, in the given order.
  /// Any IDs in the cycler that are not present in `orderedIds` are appended
  /// at the end, sorted by priority. IDs not in the cycler are ignored.
  ///
  /// @param orderedIds the desired leading order
  void setCustomOrder(@NonNull List<@NonNull Identifier> orderedIds);

  /// Returns an unmodifiable list of all perspective IDs in the cycler.
  ///
  /// The list includes every ID added to the cycler, regardless of whether
  /// the perspective is available or registered in the {@link PerspectiveRegistry}.
  ///
  /// Ordering depends on {@link #isCustomOrderEnabled()}:
  /// - if `true`, IDs follow the order set by {@link #setCustomOrder(List)},
  ///   with any remaining IDs appended in priority order
  /// - if `false`, IDs are sorted by priority (ascending)
  ///
  /// @return an unmodifiable list of all perspective IDs in the current cycle order
  @NonNull List<@NonNull Identifier> getIds();

  /// Returns the next perspective ID after `current` in the cycle list.
  ///
  /// Wraps around: the successor of the last element is the first.
  /// If `current` is null or not in the list, returns the first element.
  ///
  /// @return the next ID, or null if the list is empty
  @Nullable Identifier getNext(@Nullable Identifier current);

  /// Returns the perspective ID before `current` in the cycle list.
  ///
  /// Wraps around: the predecessor of the first element is the last.
  /// If `current` is null or not in the list, returns the last element.
  ///
  /// @return the previous ID, or null if the list is empty
  @Nullable Identifier getPrevious(@Nullable Identifier current);

  /// Sets the active perspective ID. Pass null to clear the active perspective.
  void setActiveId(@Nullable Identifier id);

  /// Advances the active perspective to the next registered one in the cycle list.
  ///
  /// Iterates forward through the list, skipping IDs not present in the
  /// {@link PerspectiveRegistry}. If the active ID is null or not in the list, cycling starts from
  /// the first element.
  void cycleForward();

  /// Moves the active perspective to the previous registered one in the cycle list.
  ///
  /// Iterates backward through the list, skipping IDs not present in the
  /// {@link PerspectiveRegistry}. If the active ID is null or not in the list, cycling starts from
  /// the last element.
  void cycleBackward();
}
