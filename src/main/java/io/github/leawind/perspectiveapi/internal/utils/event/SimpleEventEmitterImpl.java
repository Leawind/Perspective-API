package io.github.leawind.perspectiveapi.internal.utils.event;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class SimpleEventEmitterImpl<E> implements SimpleEventEmitter.Owned<E> {

  private final Collection<Listener<E>> listeners;

  public SimpleEventEmitterImpl() {
    this.listeners = new ArrayList<>();
  }

  public SimpleEventEmitterImpl(Collection<Listener<E>> listeners) {
    this.listeners = listeners;
  }

  @Override
  public void clear() {
    listeners.clear();
  }

  @Override
  public void on(Listener<E> listener) {
    listeners.add(listener);
  }

  @Override
  public void on(Listener.NoArg<E> listener) {
    listeners.add(listener);
  }

  @Override
  public void emit() {
    emit(null);
  }

  @Override
  public void emit(@Nullable E event) {
    listeners.forEach(listener -> listener.on(event));
  }
}
