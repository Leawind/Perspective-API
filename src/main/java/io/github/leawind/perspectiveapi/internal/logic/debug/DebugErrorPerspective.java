package io.github.leawind.perspectiveapi.internal.logic.debug;

import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveContext;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import net.minecraft.network.chat.Component;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

/// Development-only perspective for observing quaternion length errors.
public final class DebugErrorPerspective implements PerspectiveBehavior {
  public static final String ID = "perspective_api.debug_error";
  public static final DebugErrorPerspective INSTANCE = new DebugErrorPerspective();
  public static final PerspectiveInfo INFO =
      PerspectiveInfo.builder(ID, Component.literal("Debug Quaternion Error"))
          .baseType(BaseType.THIRD_PERSON_BACK)
          .priority(1000)
          .trait("third_person")
          .build();

  static final double LENGTH_SQUARED_ERROR_AMPLITUDE = 0.00316f;
  static final double ERROR_PERIOD_SECONDS = 0.2;

  private DebugErrorPerspective() {}

  @Override
  public void applyCameraState(
      PerspectiveState.@NonNull Mutable state, @NonNull PerspectiveContext context) {
    applyRotationError(state.rotation(), GLFW.glfwGetTime());
  }

  static double timeFactor(double timeSeconds) {
    return Math.sin(timeSeconds * 2.0 * Math.PI / ERROR_PERIOD_SECONDS);
  }

  static float lengthSquaredErrorAt(double timeSeconds) {
    return (float) (LENGTH_SQUARED_ERROR_AMPLITUDE * timeFactor(timeSeconds));
  }

  static void applyRotationError(Quaternionf rotation, double timeSeconds) {
    float lengthSquaredScale = 1.0f + lengthSquaredErrorAt(timeSeconds);
    rotation.mul((float) Math.sqrt(lengthSquaredScale));
  }
}
