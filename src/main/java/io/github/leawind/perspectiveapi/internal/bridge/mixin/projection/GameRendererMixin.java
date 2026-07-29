package io.github.leawind.perspectiveapi.internal.bridge.mixin.projection;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

/*? if >=26.1 {*/
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.ProjectionType;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraProjectionAccessor;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
/*? } else {*/
/*import io.github.leawind.perspectiveapi.internal.bridge.ProjectionBridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyProjectionContext;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
/^? if >=1.21.11 {^/
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.ProjectionType;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.ModifyArg;
/^? } else {^/
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.spongepowered.asm.mixin.injection.Redirect;
/^? }^/
*//*? }*/

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  /*? if >=26.1 {*/
  @Shadow @Final private Camera mainCamera;

  @ModifyArg(
      method = "renderLevel",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V",
              ordinal = 0),
      index = 1)
  private ProjectionType perspective_api$modifyWorldProjectionType(ProjectionType original) {
    return CameraProjectionAccessor.of(mainCamera).perspective_api$isOrthographic()
        ? ProjectionType.ORTHOGRAPHIC
        : original;
  }

  @ModifyReturnValue(method = "projectHorizonToScreen", at = @At("RETURN"))
  private double perspective_api$modifyProjectedHorizon(double original) {
    if (!CameraProjectionAccessor.of(mainCamera).perspective_api$isOrthographic()) return original;

    float xRotDeg = mainCamera.xRot();
    if (xRotDeg < 0) return Double.NEGATIVE_INFINITY;
    if (xRotDeg > 0) return Double.POSITIVE_INFINITY;
    return 0;
  }
  /*? } else if >=1.21.11 {*/
  /*@Shadow
  public abstract float getDepthFar();

  @Shadow @Final private Camera mainCamera;

  @Unique private final ModifyProjectionContext perspective_api$projectionContext =
      new ModifyProjectionContext();
  @Unique private boolean perspective_api$orthographic;

  @ModifyExpressionValue(
      method = "renderLevel",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(F)Lorg/joml/Matrix4f;"))
  private Matrix4f perspective_api$modifyWorldProjection(Matrix4f original) {
    return perspective_api$modifyProjection(original);
  }

  @ModifyExpressionValue(
      method = "projectPointToScreen",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(F)Lorg/joml/Matrix4f;"))
  private Matrix4f perspective_api$modifyScreenProjection(Matrix4f original) {
    return perspective_api$modifyProjection(original);
  }

  @ModifyReturnValue(method = "getProjectionMatrixForCulling", at = @At("RETURN"))
  private Matrix4f perspective_api$modifyProjectionForCulling(Matrix4f original) {
    return perspective_api$modifyProjection(original);
  }

  @ModifyArg(
      method = "renderLevel",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V",
              ordinal = 0),
      index = 1)
  private ProjectionType perspective_api$modifyWorldProjectionType(ProjectionType original) {
    return perspective_api$orthographic ? ProjectionType.ORTHOGRAPHIC : original;
  }

  @ModifyReturnValue(method = "projectHorizonToScreen", at = @At("RETURN"))
  private double perspective_api$modifyProjectedHorizon(double original) {
    perspective_api$updateProjectionState();
    if (!perspective_api$orthographic) return original;

    float xRotDeg = mainCamera.xRot();
    if (xRotDeg < 0) return Double.NEGATIVE_INFINITY;
    if (xRotDeg > 0) return Double.POSITIVE_INFINITY;
    return 0;
  }

  @Unique
  private Matrix4f perspective_api$modifyProjection(Matrix4f original) {
    perspective_api$updateProjectionState();
    if (!perspective_api$orthographic) return original;

    return ProjectionBridge.setCenteredOrthographic(
        original.identity(),
        perspective_api$projectionContext.orthographicHeight,
        perspective_api$aspectRatio(),
        0.05f,
        getDepthFar(),
        false);
  }

  @Unique
  private void perspective_api$updateProjectionState() {
    perspective_api$projectionContext.setup();
    GameClientEvents.MODIFY_PROJECTION.emit(perspective_api$projectionContext);
    perspective_api$orthographic = perspective_api$projectionContext.orthographic;
  }

  @Unique
  private static float perspective_api$aspectRatio() {
    var window = Minecraft.getInstance().getWindow();
    return (float) window.getWidth() / window.getHeight();
  }
  *//*? } else {*/
  /*@Shadow
  public abstract float getDepthFar();

  @Unique private final ModifyProjectionContext perspective_api$projectionContext =
      new ModifyProjectionContext();
  @Unique private boolean perspective_api$orthographic;

  @Redirect(
      method = "renderLevel",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/GameRenderer;getProjectionMatrix(D)Lorg/joml/Matrix4f;"))
  private Matrix4f perspective_api$modifyProjection(
      GameRenderer renderer, double fovDeg) {
    Matrix4f original = renderer.getProjectionMatrix(fovDeg);
    perspective_api$projectionContext.setup();
    GameClientEvents.MODIFY_PROJECTION.emit(perspective_api$projectionContext);
    perspective_api$orthographic = perspective_api$projectionContext.orthographic;
    if (!perspective_api$orthographic) return original;

    var window = Minecraft.getInstance().getWindow();
    return ProjectionBridge.setCenteredOrthographic(
        original.identity(),
        perspective_api$projectionContext.orthographicHeight,
        (float) window.getWidth() / window.getHeight(),
        0.05f,
        getDepthFar(),
        false);
  }

  @Redirect(
      method = "renderLevel",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/renderer/GameRenderer;resetProjectionMatrix(Lorg/joml/Matrix4f;)V"))
  private void perspective_api$setWorldProjection(
      GameRenderer renderer, Matrix4f projectionMatrix) {
    if (perspective_api$orthographic) {
      RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);
    } else {
      renderer.resetProjectionMatrix(projectionMatrix);
    }
  }
  *//*? }*/
}
