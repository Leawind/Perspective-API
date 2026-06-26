package io.github.leawind.perspectiveapi.internal.utils.event;

import org.jspecify.annotations.Nullable;

/**
 * A simple event emitter that supports both one-time and persistent listeners.
 *
 * @param <E> the event type
 */
@SuppressWarnings("unused")
public class SingleEventEmitter<E> {
  protected boolean isOnce;
  protected @Nullable Listener<E> listener;

  /**
   * Clears the current listener.
   *
   * @return this emitter for method chaining
   */
  public SingleEventEmitter<E> clear() {
    listener = null;
    return this;
  }

  /**
   * Checks if a listener is registered.
   *
   * @return true if a listener exists, false otherwise
   */
  public boolean hasListener() {
    return listener != null;
  }

  /**
   * Gets the current listener.
   *
   * @return the current listener, or null if none
   */
  public @Nullable Listener<E> getListener() {
    return listener;
  }

  /**
   * Registers a one-time listener that will be called once and then removed.
   *
   * @param listener the listener to register
   * @return this emitter for method chaining
   */
  public SingleEventEmitter<E> once(Listener.NoArg<E> listener) {
    isOnce = true;
    this.listener = listener;
    return this;
  }

  /**
   * Registers a one-time listener that will be called once and then removed.
   *
   * @param listener the listener to register
   * @return this emitter for method chaining
   */
  public SingleEventEmitter<E> once(Listener<E> listener) {
    isOnce = true;
    this.listener = listener;
    return this;
  }

  /**
   * Registers a persistent listener that will be called on every emit.
   *
   * @param listener the listener to register
   * @return this emitter for method chaining
   */
  public SingleEventEmitter<E> on(Listener.NoArg<E> listener) {
    isOnce = false;
    this.listener = listener;
    return this;
  }

  /**
   * Registers a persistent listener that will be called on every emit.
   *
   * @param listener the listener to register
   * @return this emitter for method chaining
   */
  public SingleEventEmitter<E> on(Listener<E> listener) {
    isOnce = false;
    this.listener = listener;
    return this;
  }

  /**
   * Removes the current listener.
   *
   * @return this emitter for method chaining
   */
  public SingleEventEmitter<E> off() {
    listener = null;
    return this;
  }

  /**
   * Wraps a no-arg listener.
   *
   * @param listener the listener to wrap
   * @return the wrapped listener
   */
  public Listener<E> listener(Listener.NoArg<E> listener) {
    return listener;
  }

  /** Emits an event with null payload. */
  public void emit() {
    emit(null);
  }

  /**
   * Emits an event to the registered listener. If the listener was registered with once(), it will
   * be removed after emission.
   *
   * @param event the event payload, may be null
   */
  public void emit(@Nullable E event) {
    if (listener != null) {
      listener.on(event);
      if (isOnce) {
        off();
      }
    }
  }
}
