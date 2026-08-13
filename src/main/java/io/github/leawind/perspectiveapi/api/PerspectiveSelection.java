package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Stores the player's persistent perspective selection.
///
/// The built-in perspective switcher and other selection logic update this shared state directly.
/// Temporary overrides do not modify it. The raw selected ID remains stored when its perspective is
/// unavailable or unregistered, while camera resolution safely falls back to the default
/// perspective.
@ApiStatus.Experimental
@ApiStatus.NonExtendable
public interface PerspectiveSelection {
  /// Returns the raw selected perspective ID, or `null` when no selection is stored.
  @Nullable String get();

  /// Stores a perspective ID as the current persistent selection.
  ///
  /// Selecting the already stored ID has no effect and does not notify listeners.
  void set(@Nullable String perspectiveId);

  /// Receives a changed raw perspective ID after the new value has been stored. Owns one
  /// change-listener subscription.
  @ApiStatus.NonExtendable
  interface ListenerRegistration {

    /// Removes this exact listener registration.
    ///
    /// @return `true` if the registration was removed, or `false` if it was already absent
    boolean unregister();
  }

  @FunctionalInterface
  interface Listener {

    /// Called when the selected ID has changed.
    ///
    /// The ID can be `null` when persisted state with no selection is restored. Listener failures
    /// are logged and do not prevent other listeners from being notified.
    void onChanged(@Nullable String perspectiveId);
  }

  /// Registers a listener for actual selected-ID changes.
  ///
  /// Notifications are synchronous and ordered. A selection changed by a listener is queued until
  /// the current notification finishes, preventing older notifications from overwriting newer state
  /// in later listeners.
  @NonNull ListenerRegistration onChanged(@NonNull Listener listener);
}
