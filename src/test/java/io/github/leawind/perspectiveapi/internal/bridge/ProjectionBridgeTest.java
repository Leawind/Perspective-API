package io.github.leawind.perspectiveapi.internal.bridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class ProjectionBridgeTest {
  @Test
  void identifiesOrthographicFrustumMatrixAfterViewRotation() {
    Matrix4f matrix =
        new Matrix4f()
            .setOrtho(-8.0f, 8.0f, -5.0f, 5.0f, 0.05f, 512.0f)
            .mul(new Matrix4f().rotationXYZ(0.7f, 1.2f, 0.0f));

    assertTrue(ProjectionBridge.isOrthographicFrustumMatrix(matrix));
  }

  @Test
  void rejectsPerspectiveFrustumMatrixAfterViewRotation() {
    Matrix4f matrix =
        new Matrix4f()
            .setPerspective((float) Math.toRadians(70.0), 1.6f, 0.05f, 512.0f)
            .mul(new Matrix4f().rotationXYZ(0.7f, 1.2f, 0.0f));

    assertFalse(ProjectionBridge.isOrthographicFrustumMatrix(matrix));
  }
}
