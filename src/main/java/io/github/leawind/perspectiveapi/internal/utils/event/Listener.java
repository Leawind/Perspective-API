package io.github.leawind.perspectiveapi.internal.utils.event;

public interface Listener<E> {
  void on(E event);

  interface NoArg<E> extends Listener<E> {
    void on();

    @Override
    default void on(E event) {
      on();
    }
  }
}
