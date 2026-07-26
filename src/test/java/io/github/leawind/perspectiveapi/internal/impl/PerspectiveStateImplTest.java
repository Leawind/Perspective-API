package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.perspectiveapi.testutils.TestUtils;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class PerspectiveStateImplTest {
  @Test
  void startsWithDocumentedDefaults() {
    PerspectiveStateImpl state = new PerspectiveStateImpl();

    TestUtils.assertVectorEquals(new Vector3d(), state.position());
    TestUtils.assertQuatEquals(new Quaternionf(), state.rotation());
    assertEquals(PerspectiveStateImpl.DEFAULT_FOV_DEGREES, state.getFovDeg());
  }

  @Test
  void mutableAccessorsExposeStableObjects() {
    PerspectiveStateImpl state = new PerspectiveStateImpl();

    assertSame(state.position(), state.position());
    assertSame(state.rotation(), state.rotation());

    state.position().set(1.0, 2.0, 3.0);
    state.rotation().rotationY(0.5f);
    state.setFovDeg(95.0f);

    TestUtils.assertVectorEquals(new Vector3d(1.0, 2.0, 3.0), state.position());
    TestUtils.assertQuatEquals(new Quaternionf().rotationY(0.5f), state.rotation());
    assertEquals(95.0f, state.getFovDeg());
  }

  @Test
  void setCopiesValuesWithoutAliasing() {
    PerspectiveStateImpl source = new PerspectiveStateImpl();
    source.position().set(1.0, 2.0, 3.0);
    source.rotation().rotationXYZ(0.1f, 0.2f, 0.3f);
    source.setFovDeg(80.0f);
    PerspectiveStateImpl copy = new PerspectiveStateImpl();

    assertSame(copy, copy.set(source));

    assertNotSame(source.position(), copy.position());
    assertNotSame(source.rotation(), copy.rotation());
    TestUtils.assertVectorEquals(source.position(), copy.position());
    TestUtils.assertQuatEquals(source.rotation(), copy.rotation());
    assertEquals(source.getFovDeg(), copy.getFovDeg());

    source.position().zero();
    source.rotation().identity();
    source.setFovDeg(1.0f);
    TestUtils.assertVectorEquals(new Vector3d(1.0, 2.0, 3.0), copy.position());
    assertEquals(80.0f, copy.getFovDeg());
  }

  @Test
  void setRejectsNull() {
    assertThrows(NullPointerException.class, () -> new PerspectiveStateImpl().set(null));
  }
}
