package io.github.leawind.perspectiveapi.internal.bridge.mixin.projection;

import io.github.leawind.perspectiveapi.internal.bridge.ProjectionBridge;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public abstract class FrustumMixin {
  @Shadow @Final private Matrix4f matrix;

  /// Vanilla repeatedly moves a perspective frustum backwards until it fully contains the camera
  /// cube. An orthographic frustum does not widen when moved, so that loop may never terminate.
  @Inject(method = "offsetToFullyIncludeCameraCube", at = @At("HEAD"), cancellable = true)
  private void perspective_api$skipOrthographicCameraCubeOffset(
      int cubeSize, CallbackInfoReturnable<Frustum> cir) {
    if (ProjectionBridge.isOrthographicFrustumMatrix(matrix)) {
      cir.setReturnValue((Frustum) (Object) this);
    }
  }
}
