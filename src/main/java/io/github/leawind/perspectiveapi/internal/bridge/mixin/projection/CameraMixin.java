package io.github.leawind.perspectiveapi.internal.bridge.mixin.projection;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;

/*? if >=26.1 {*/
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.leawind.perspectiveapi.internal.bridge.ProjectionBridge;
import io.github.leawind.perspectiveapi.internal.bridge.access.CameraProjectionAccessor;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyProjectionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/*? } else {*/
/*import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.ModifyProjectionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*//*? }*/

@Mixin(Camera.class)
public abstract class CameraMixin
    /*? if >=26.1 {*/ implements CameraProjectionAccessor /*? }*/ {
  /*? if >=26.1 {*/
  @Shadow @Final private Vector3f forwards;
  @Shadow @Final private Vector3f up;
  @Shadow @Final private Vector3f left;
  @Shadow private float depthFar;

  @Shadow
  public abstract Matrix4f getViewRotationMatrix(Matrix4f dest);

  @Unique private final ModifyProjectionContext perspective_api$projectionContext =
      new ModifyProjectionContext();
  @Unique private boolean perspective_api$orthographic;
  @Unique private float perspective_api$orthographicHeight;

  @ModifyReturnValue(method = "createProjectionMatrixForCulling", at = @At("RETURN"))
  private Matrix4f perspective_api$modifyProjectionForCulling(Matrix4f original) {
    perspective_api$updateProjectionState();
    if (!perspective_api$orthographic) return original;

    return ProjectionBridge.setCenteredOrthographic(
        original.identity(),
        perspective_api$orthographicHeight,
        perspective_api$aspectRatio(),
        0.05f,
        depthFar,
        false);
  }

  @Inject(method = "extractRenderState", at = @At("RETURN"))
  private void perspective_api$modifyExtractedProjection(
      CameraRenderState cameraState, float cameraEntityPartialTicks, CallbackInfo ci) {
    if (!perspective_api$orthographic) return;

    ProjectionBridge.setCenteredOrthographic(
        cameraState.projectionMatrix,
        perspective_api$orthographicHeight,
        perspective_api$aspectRatio(),
        0.05f,
        depthFar,
        true);
  }

  @ModifyReturnValue(method = "getViewRotationProjectionMatrix", at = @At("RETURN"))
  private Matrix4f perspective_api$modifyViewRotationProjection(Matrix4f original) {
    if (!perspective_api$orthographic) return original;

    ProjectionBridge.setCenteredOrthographic(
        original,
        perspective_api$orthographicHeight,
        perspective_api$aspectRatio(),
        0.05f,
        depthFar,
        true);
    return original.mul(getViewRotationMatrix(new Matrix4f()));
  }

  @ModifyReturnValue(method = "getNearPlane", at = @At("RETURN"))
  private Camera.NearPlane perspective_api$modifyNearPlane(
      Camera.NearPlane original, float fov) {
    if (!perspective_api$orthographic) return original;

    double halfHeight = perspective_api$orthographicHeight * 0.5;
    double halfWidth = halfHeight * perspective_api$aspectRatio();
    return CameraNearPlaneInvoker.perspective_api$create(
        new Vec3(forwards).scale(0.05),
        new Vec3(left).scale(halfWidth),
        new Vec3(up).scale(halfHeight));
  }

  @Override
  public boolean perspective_api$isOrthographic() {
    return perspective_api$orthographic;
  }

  @Unique
  private void perspective_api$updateProjectionState() {
    perspective_api$projectionContext.setup();
    GameClientEvents.MODIFY_PROJECTION.emit(perspective_api$projectionContext);
    perspective_api$orthographic = perspective_api$projectionContext.orthographic;
    perspective_api$orthographicHeight =
        perspective_api$projectionContext.orthographicHeight;
  }

  @Unique
  private static float perspective_api$aspectRatio() {
    var window = Minecraft.getInstance().getWindow();
    return (float) window.getWidth() / window.getHeight();
  }
  /*? } else {*/
  /*@Shadow @Final private Vector3f forwards;
  @Shadow @Final private Vector3f up;
  @Shadow @Final private Vector3f left;

  @Unique private final ModifyProjectionContext perspective_api$projectionContext =
      new ModifyProjectionContext();

  @Inject(method = "getNearPlane", at = @At("RETURN"), cancellable = true)
  private void perspective_api$modifyNearPlane(
      CallbackInfoReturnable<Camera.NearPlane> cir) {
    perspective_api$projectionContext.setup();
    GameClientEvents.MODIFY_PROJECTION.emit(perspective_api$projectionContext);
    if (!perspective_api$projectionContext.orthographic) return;

    var window = Minecraft.getInstance().getWindow();
    double halfHeight = perspective_api$projectionContext.orthographicHeight * 0.5;
    double halfWidth = halfHeight * window.getWidth() / window.getHeight();
    cir.setReturnValue(
        CameraNearPlaneInvoker.perspective_api$create(
            new Vec3(forwards).scale(0.05),
            new Vec3(left).scale(halfWidth),
            new Vec3(up).scale(halfHeight)));
  }
  *//*? }*/
}
