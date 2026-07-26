package io.github.leawind.perspectiveapi.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InitializationCoordinatorTest {
  private InitializationCoordinator coordinator;

  @BeforeEach
  void beforeEach() {
    coordinator = new InitializationCoordinator();
  }

  @Test
  void actionWaitsUntilInitializationFinishes() {
    AtomicInteger calls = new AtomicInteger();
    coordinator.runWhenReady("test.queued", calls::incrementAndGet);

    assertEquals(0, calls.get());

    coordinator.finish();
    assertEquals(1, calls.get());
  }

  @Test
  void actionRunsImmediatelyAfterInitializationFinishes() {
    coordinator.finish();

    AtomicInteger calls = new AtomicInteger();
    coordinator.runWhenReady("test.immediate", calls::incrementAndGet);

    assertEquals(1, calls.get());
  }

  @Test
  void finishIsIdempotent() {
    AtomicInteger calls = new AtomicInteger();
    coordinator.runWhenReady("test.once", calls::incrementAndGet);

    coordinator.finish();
    coordinator.finish();

    assertEquals(1, calls.get());
  }

  @Test
  void nestedActionRunsExactlyOnce() {
    AtomicInteger calls = new AtomicInteger();
    coordinator.runWhenReady(
        "test.outer",
        () -> {
          calls.incrementAndGet();
          coordinator.runWhenReady("test.inner", calls::incrementAndGet);
        });

    coordinator.finish();

    assertEquals(2, calls.get());
  }

  @Test
  void queuedFailuresDoNotPreventRemainingActions() {
    AtomicInteger calls = new AtomicInteger();
    coordinator.runWhenReady(
        "test.first",
        () -> {
          throw new IllegalArgumentException("first");
        });
    coordinator.runWhenReady("test.second", calls::incrementAndGet);
    coordinator.runWhenReady(
        "test.third",
        () -> {
          throw new IllegalStateException("third");
        });

    RuntimeException exception = assertThrows(RuntimeException.class, coordinator::finish);

    assertEquals(1, calls.get());
    assertEquals(2, exception.getSuppressed().length);
    assertEquals(
        "Perspective API initialization action 'test.first' failed",
        exception.getSuppressed()[0].getMessage());
    assertEquals(
        "Perspective API initialization action 'test.third' failed",
        exception.getSuppressed()[1].getMessage());
  }

  @Test
  void immediateFailureIncludesActionKey() {
    coordinator.finish();

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                coordinator.runWhenReady(
                    "test.failure",
                    () -> {
                      throw new IllegalArgumentException("failure");
                    }));

    assertEquals(
        "Perspective API initialization action 'test.failure' failed", exception.getMessage());
    assertEquals("failure", exception.getCause().getMessage());
  }

  @Test
  void failedFinishStillMarksCoordinatorReady() {
    coordinator.runWhenReady(
        "test.failure",
        () -> {
          throw new IllegalStateException("failure");
        });
    assertThrows(RuntimeException.class, coordinator::finish);
    AtomicInteger calls = new AtomicInteger();

    coordinator.runWhenReady("test.after-failure", calls::incrementAndGet);

    assertEquals(1, calls.get());
  }

  @Test
  void fatalFailureIsRethrownUnwrapped() {
    TestVirtualMachineError fatal = new TestVirtualMachineError();
    coordinator.runWhenReady(
        "test.fatal",
        () -> {
          throw fatal;
        });

    assertSame(fatal, assertThrows(TestVirtualMachineError.class, coordinator::finish));
  }

  @Test
  void concurrentActionsRunExactlyOnce() throws Exception {
    int actionCount = 100;
    AtomicInteger calls = new AtomicInteger();
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(8);
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < actionCount; i++) {
        futures.add(
            executor.submit(
                () -> {
                  await(start);
                  coordinator.runWhenReady("test.concurrent", calls::incrementAndGet);
                }));
      }
      futures.add(
          executor.submit(
              () -> {
                await(start);
                coordinator.finish();
              }));

      start.countDown();
      for (Future<?> future : futures) {
        assertDoesNotThrow(() -> get(future));
      }
    } finally {
      executor.shutdownNow();
    }

    assertEquals(actionCount, calls.get());
  }

  @Test
  void rejectsNullArguments() {
    assertThrows(NullPointerException.class, () -> coordinator.runWhenReady(null, () -> {}));
    assertThrows(NullPointerException.class, () -> coordinator.runWhenReady("test.null", null));
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    }
  }

  private static void get(Future<?> future) {
    try {
      future.get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    } catch (ExecutionException exception) {
      throw new RuntimeException(exception.getCause());
    }
  }

  private static final class TestVirtualMachineError extends VirtualMachineError {}
}
