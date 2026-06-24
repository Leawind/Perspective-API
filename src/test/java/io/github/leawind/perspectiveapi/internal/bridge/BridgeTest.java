package io.github.leawind.perspectiveapi.internal.bridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BridgeTest {
  @Test
  void testGetFov() {
    assertTrue(Float.isFinite(Bridge.getCurrentFov()));
  }
}
