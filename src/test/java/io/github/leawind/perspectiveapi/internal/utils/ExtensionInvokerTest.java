package io.github.leawind.perspectiveapi.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ExtensionInvokerTest {
  private ExtensionInvoker invoker;

  @BeforeEach
  void beforeEach() {
    invoker =
        new ExtensionInvoker(LoggerFactory.getLogger(ExtensionInvokerTest.class), "Test extension");
  }

  @Test
  void runReportsSuccessAndIsolatesNonFatalFailure() {
    AtomicInteger calls = new AtomicInteger();

    assertTrue(invoker.run("test", "run", calls::incrementAndGet));
    assertFalse(
        invoker.run(
            "test",
            "run-failure",
            () -> {
              throw new IllegalStateException("failure");
            }));
    assertEquals(1, calls.get());
  }

  @Test
  void testOrElseReturnsResultOrFallback() {
    assertTrue(invoker.testOrElse("test", "true", () -> true, false));
    assertFalse(invoker.testOrElse("test", "false", () -> false, true));
    assertTrue(
        invoker.testOrElse(
            "test",
            "failure",
            () -> {
              throw new AssertionError("failure");
            },
            true));
  }

  @Test
  void callOrElseSupportsValuesNullAndFallback() {
    assertEquals("value", invoker.callOrElse("test", "value", () -> "value", "fallback"));
    assertNull(invoker.callOrElse("test", "null", () -> null, "fallback"));
    assertEquals(
        "fallback",
        invoker.callOrElse(
            "test",
            "failure",
            () -> {
              throw new IllegalStateException("failure");
            },
            "fallback"));
  }

  @Test
  void fatalErrorsAreRethrown() {
    TestVirtualMachineError fatal = new TestVirtualMachineError();

    assertSame(
        fatal,
        assertThrows(
            TestVirtualMachineError.class,
            () ->
                invoker.run(
                    "test",
                    "fatal",
                    () -> {
                      throw fatal;
                    })));
  }

  @Test
  void rejectsNullRequiredArguments() {
    assertThrows(NullPointerException.class, () -> new ExtensionInvoker(null, "test"));
    assertThrows(
        NullPointerException.class,
        () -> new ExtensionInvoker(LoggerFactory.getLogger(ExtensionInvokerTest.class), null));
    assertThrows(NullPointerException.class, () -> invoker.run(null, "phase", () -> {}));
    assertThrows(NullPointerException.class, () -> invoker.run("id", null, () -> {}));
    assertThrows(NullPointerException.class, () -> invoker.run("id", "phase", null));
    assertThrows(NullPointerException.class, () -> invoker.testOrElse("id", "phase", null, false));
    assertThrows(
        NullPointerException.class, () -> invoker.callOrElse("id", "phase", null, "fallback"));
    assertThrows(NullPointerException.class, () -> invoker.report("id", "phase", null));
  }

  private static final class TestVirtualMachineError extends VirtualMachineError {}
}
