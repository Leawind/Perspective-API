package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.ProjectionMode;
import io.github.leawind.perspectiveapi.testutils.TestUtils;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

class PerspectiveStateSnapshotTest {
  @Test
  void copiesAllFieldsAndDoesNotTrackSourceChanges() {
    PerspectiveStateImpl source = new PerspectiveStateImpl();
    source.position().set(1.0, 2.0, 3.0);
    source.rotation().rotationYXZ(0.1f, 0.2f, 0.3f);
    source.setProjectionMode(ProjectionMode.ORTHOGRAPHIC);
    source.setFovDeg(55.0f);
    source.setOrthographicHeight(24.0f);

    PerspectiveState snapshot = new PerspectiveStateSnapshot(source);
    source.position().zero();
    source.rotation().identity();
    source.setProjectionMode(ProjectionMode.PERSPECTIVE);
    source.setFovDeg(70.0f);
    source.setOrthographicHeight(8.0f);

    TestUtils.assertVectorEquals(new Vector3d(1.0, 2.0, 3.0), snapshot.position());
    TestUtils.assertQuatEquals(
        new Quaternionf().rotationYXZ(0.1f, 0.2f, 0.3f), snapshot.rotation());
    assertEquals(ProjectionMode.ORTHOGRAPHIC, snapshot.projectionMode());
    assertEquals(55.0f, snapshot.getFovDeg());
    assertEquals(24.0f, snapshot.getOrthographicHeight());
  }

  @Test
  void rejectsNullSource() {
    assertThrows(NullPointerException.class, () -> new PerspectiveStateSnapshot(null));
  }
}
