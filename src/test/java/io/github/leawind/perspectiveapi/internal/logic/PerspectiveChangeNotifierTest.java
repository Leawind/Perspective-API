package io.github.leawind.perspectiveapi.internal.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveChangeListenerRegistration;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class PerspectiveChangeNotifierTest {

  private static Perspective perspective(String id) {
    PerspectiveInfo info = PerspectiveInfo.builder(id, Component.literal(id)).build();
    return new Perspective() {
      @Override
      public @NonNull PerspectiveInfo info() {
        return info;
      }

      @Override
      public boolean isAvailable() {
        return true;
      }
    };
  }

  @Test
  void notifiesListenersWithFromAndTo() {
    PerspectiveChangeNotifier notifier = new PerspectiveChangeNotifier();
    List<Perspective> received = new ArrayList<>();
    notifier.subscribe(
        (from, to) -> {
          received.add(from);
          received.add(to);
        });

    Perspective first = perspective("test.first");
    Perspective second = perspective("test.second");
    notifier.notifyChanged(null, first);
    notifier.notifyChanged(first, second);
    notifier.notifyChanged(second, null);

    assertEquals(Arrays.asList(null, first, first, second, second, null), received);
  }

  @Test
  void listenerFailuresDoNotPreventLaterListeners() {
    PerspectiveChangeNotifier notifier = new PerspectiveChangeNotifier();
    AtomicInteger successfulCalls = new AtomicInteger();
    notifier.subscribe(
        (from, to) -> {
          throw new IllegalStateException("listener failure");
        });
    notifier.subscribe((from, to) -> successfulCalls.incrementAndGet());

    notifier.notifyChanged(null, perspective("test.selected"));

    assertEquals(1, successfulCalls.get());
  }

  @Test
  void reentrantChangesAreQueuedUntilCurrentNotificationCompletes() {
    PerspectiveChangeNotifier notifier = new PerspectiveChangeNotifier();
    Perspective first = perspective("test.first");
    Perspective second = perspective("test.second");
    List<String> calls = new ArrayList<>();
    notifier.subscribe(
        (from, to) -> {
          calls.add("first:" + to.info().id());
          if (to == first) notifier.notifyChanged(first, second);
        });
    notifier.subscribe((from, to) -> calls.add("second:" + to.info().id()));

    notifier.notifyChanged(null, first);

    assertEquals(
        List.of("first:test.first", "second:test.first", "first:test.second", "second:test.second"),
        calls);
  }

  @Test
  void registrationUnregistersOnlyItself() {
    PerspectiveChangeNotifier notifier = new PerspectiveChangeNotifier();
    AtomicInteger firstCalls = new AtomicInteger();
    AtomicInteger secondCalls = new AtomicInteger();
    PerspectiveChangeListenerRegistration firstRegistration =
        notifier.subscribe((from, to) -> firstCalls.incrementAndGet());
    notifier.subscribe((from, to) -> secondCalls.incrementAndGet());

    assertTrue(firstRegistration.unregister());
    assertFalse(firstRegistration.unregister());
    notifier.notifyChanged(null, perspective("test.selected"));

    assertEquals(0, firstCalls.get());
    assertEquals(1, secondCalls.get());
  }

  @Test
  void changeCarriesExactPerspectiveInstances() {
    PerspectiveChangeNotifier notifier = new PerspectiveChangeNotifier();
    Perspective from = perspective("test.from");
    Perspective to = perspective("test.to");
    List<Perspective> received = new ArrayList<>();
    notifier.subscribe(
        (fromP, toP) -> {
          received.add(fromP);
          received.add(toP);
        });

    notifier.notifyChanged(from, to);

    assertEquals(2, received.size());
    assertSame(from, received.get(0));
    assertSame(to, received.get(1));
  }
}
