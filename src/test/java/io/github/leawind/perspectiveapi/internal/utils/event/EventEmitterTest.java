package io.github.leawind.perspectiveapi.internal.utils.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EventEmitterTest {
  @Test
  void simpleEmitterNotifiesPayloadAndNoArgListenersInOrder() {
    SimpleEventEmitter.Owned<String> emitter = SimpleEventEmitter.create();
    List<String> calls = new ArrayList<>();
    emitter.on(event -> calls.add("payload:" + event));
    emitter.on(() -> calls.add("no-arg"));

    emitter.emit("value");

    assertEquals(List.of("payload:value", "no-arg"), calls);
  }

  @Test
  void simpleEmitterCanEmitNullAndClearListeners() {
    SimpleEventEmitter.Owned<String> emitter = SimpleEventEmitter.create();
    AtomicReference<String> payload = new AtomicReference<>("initial");
    emitter.on(payload::set);

    emitter.emit();
    assertNull(payload.get());

    emitter.clear();
    emitter.emit("ignored");
    assertNull(payload.get());
  }

  @Test
  void simpleEmitterUsesProvidedListenerCollection() {
    List<SimpleEventEmitter.Listener<String>> listeners = new ArrayList<>();
    AtomicInteger calls = new AtomicInteger();
    listeners.add(event -> calls.incrementAndGet());
    SimpleEventEmitter.Owned<String> emitter = SimpleEventEmitter.create(listeners);

    emitter.emit("first");
    listeners.add(event -> calls.incrementAndGet());
    emitter.emit("second");

    assertEquals(3, calls.get());
  }

  @Test
  void singleEmitterSupportsPersistentAndOneTimeListeners() {
    SingleEventEmitter<String> emitter = new SingleEventEmitter<>();
    AtomicInteger calls = new AtomicInteger();
    AtomicReference<String> payload = new AtomicReference<>();

    assertSame(
        emitter,
        emitter.on(
            event -> {
              calls.incrementAndGet();
              payload.set(event);
            }));
    emitter.emit("first");
    emitter.emit("second");
    assertEquals(2, calls.get());
    assertEquals("second", payload.get());
    assertTrue(emitter.hasListener());

    emitter.once(calls::incrementAndGet);
    emitter.emit("third");
    emitter.emit("fourth");
    assertEquals(3, calls.get());
    assertFalse(emitter.hasListener());
  }

  @Test
  void singleEmitterExposesAndClearsCurrentListener() {
    SingleEventEmitter<String> emitter = new SingleEventEmitter<>();
    Listener<String> listener = event -> {};

    emitter.once(listener);
    assertSame(listener, emitter.getListener());
    assertSame(emitter, emitter.off());
    assertNull(emitter.getListener());

    emitter.on(listener);
    assertSame(emitter, emitter.clear());
    assertFalse(emitter.hasListener());
  }
}
