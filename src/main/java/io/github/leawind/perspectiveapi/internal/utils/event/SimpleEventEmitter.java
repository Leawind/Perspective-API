package io.github.leawind.perspectiveapi.internal.utils.event;

import java.util.Collection;

/**
 * A lightweight event emitter for subscribing to and emitting events of type {@code E}.
 *
 * @param <E> the event type
 */
public interface SimpleEventEmitter<E> {
  /**
   * Creates a new event emitter with no initial listeners.
   *
   * @param <E> the event type
   * @return a new {@code Owned} emitter instance
   */
  static <E> Owned<E> create() {
    return new SimpleEventEmitterImpl<>();
  }

  /**
   * Creates a new event emitter initialized with the given listeners.
   *
   * @param <E> the event type
   * @param listeners the initial listeners to register
   * @return a new {@code Owned} emitter instance
   */
  static <E> Owned<E> create(Collection<Listener<E>> listeners) {
    return new SimpleEventEmitterImpl<>(listeners);
  }

  /**
   * Registers a listener that receives the event object.
   *
   * @param listener the listener to register
   */
  void on(Listener<E> listener);

  /**
   * Registers a no-arg listener that ignores the event object.
   *
   * @param listener the listener to register
   */
  void on(Listener.NoArg<E> listener);

  /**
   * An extended emitter that owns the ability to emit events and manage listeners.
   *
   * @param <E> the event type
   */
  interface Owned<E> extends SimpleEventEmitter<E> {

    /** Removes all registered listeners. */
    void clear();

    /** Emits an event to all registered listeners with a {@code null} event object. */
    void emit();

    /**
     * Emits the given event to all registered listeners.
     *
     * @param event the event to emit
     */
    void emit(E event);
  }

  /**
   * A listener that handles events of type {@code E}.
   *
   * @param <E> the event type
   */
  interface Listener<E> {
    /**
     * Called when an event is emitted.
     *
     * @param event the emitted event
     */
    void on(E event);

    /**
     * A listener variant that does not receive the event object.
     *
     * @param <E> the event type
     */
    interface NoArg<E> extends Listener<E> {
      /** Called when an event is emitted, ignoring the event object. */
      void on();

      @Override
      default void on(E event) {
        on();
      }
    }
  }
}
