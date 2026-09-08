package io.github.leawind.perspectiveapi.api;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/// Receives changes of the perspective that currently owns the base camera state.
///
/// @see PerspectiveAPI#onCurrentChanged
/// @see PerspectiveAPI#getCurrent
@ApiStatus.Experimental
@FunctionalInterface
public interface PerspectiveChangeListener {

  /// Called after the effective current perspective has changed.
  ///
  /// Whenever resolution produces a different perspective — through player selection, temporary
  /// overrides, availability fallback, or registry changes — the outgoing perspective's
  /// `onDeactivate` and the incoming perspective's `onActivate` run first, so {@link
  /// PerspectiveAPI#getCurrent} already returns `to` during this callback.
  ///
  /// Notifications are synchronous on the thread that produced the change. Changes triggered from
  /// inside a listener are queued until the current notification finishes. A failing listener is
  /// logged and does not prevent other listeners from being notified.
  ///
  /// @param from the previously effective perspective, or `null` before the first activation
  /// @param to the newly effective perspective, or `null` while Perspective API is disabled
  void onChanged(@Nullable Perspective from, @Nullable Perspective to);
}
