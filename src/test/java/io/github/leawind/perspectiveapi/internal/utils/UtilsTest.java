package io.github.leawind.perspectiveapi.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class UtilsTest {
  @Test
  void clampFloatHandlesBoundsAndInteriorValues() {
    assertEquals(-2.0f, Utils.clamp(-3.0f, -2.0f, 4.0f));
    assertEquals(1.5f, Utils.clamp(1.5f, -2.0f, 4.0f));
    assertEquals(4.0f, Utils.clamp(5.0f, -2.0f, 4.0f));
    assertEquals(2.0f, Utils.clamp(100.0f, 2.0f, 2.0f));
  }

  @Test
  void clampDoubleHandlesBoundsAndInteriorValues() {
    assertEquals(-2.0, Utils.clamp(-3.0, -2.0, 4.0));
    assertEquals(1.5, Utils.clamp(1.5, -2.0, 4.0));
    assertEquals(4.0, Utils.clamp(5.0, -2.0, 4.0));
    assertEquals(2.0, Utils.clamp(100.0, 2.0, 2.0));
  }

  @Test
  void clampRejectsInvalidBounds() {
    assertThrows(IllegalArgumentException.class, () -> Utils.clamp(0.0f, 2.0f, 1.0f));
    assertThrows(IllegalArgumentException.class, () -> Utils.clamp(0.0f, Float.NaN, 1.0f));
    assertThrows(IllegalArgumentException.class, () -> Utils.clamp(0.0f, 0.0f, Float.NaN));
    assertThrows(IllegalArgumentException.class, () -> Utils.clamp(0.0, 2.0, 1.0));
    assertThrows(IllegalArgumentException.class, () -> Utils.clamp(0.0, Double.NaN, 1.0));
    assertThrows(IllegalArgumentException.class, () -> Utils.clamp(0.0, 0.0, Double.NaN));
  }

  @Test
  void classAvailabilityReportsPresentAndMissingClasses() {
    assertTrue(Utils.isClassAvailable(String.class.getName()));
    assertFalse(Utils.isClassAvailable("does.not.exist.MissingClass"));
  }

  @Test
  void finiteChecksRejectEveryNonFiniteComponent() {
    assertTrue(Sanitizer.isFinite(new Vector3d(1.0, 2.0, 3.0)));
    assertFalse(Sanitizer.isFinite(new Vector3d(Double.NaN, 2.0, 3.0)));
    assertFalse(Sanitizer.isFinite(new Vector3d(1.0, Double.POSITIVE_INFINITY, 3.0)));
    assertTrue(Sanitizer.isFinite(new Quaternionf(0.0f, 0.0f, 0.0f, 1.0f)));
    assertFalse(Sanitizer.isFinite(new Quaternionf(0.0f, Float.NaN, 0.0f, 1.0f)));
    assertFalse(Sanitizer.isFinite(Float.NEGATIVE_INFINITY));
    assertFalse(Sanitizer.isFinite(Double.NaN));
  }

  @Test
  void throttledActionIsIndependentPerKey() {
    AtomicInteger calls = new AtomicInteger();
    Sanitizer.ThrottledAction action = new Sanitizer.ThrottledAction(Long.MAX_VALUE);

    action.run("first", calls::incrementAndGet);
    action.run("first", calls::incrementAndGet);
    action.run("second", calls::incrementAndGet);

    assertEquals(2, calls.get());
  }

  @Test
  void propagatePreservesUncheckedExceptionsAndWrapsCheckedExceptions() {
    IllegalStateException runtime = new IllegalStateException("runtime");
    assertSame(runtime, Exceptions.propagate(runtime));

    IOException checked = new IOException("checked");
    RuntimeException wrapped = Exceptions.propagate(checked);
    assertSame(checked, wrapped.getCause());

    AssertionError error = new AssertionError("error");
    assertSame(error, assertThrows(AssertionError.class, () -> Exceptions.propagate(error)));
  }
}
