package io.github.leawind.perspectiveapi.internal.bridge.mixin.setupcamera;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.renderer.GameRenderer;

/*? if >=1.21 {*/
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {}

/*? } else if >=1.20.6 {*/
/*import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  @Shadow @Final private Camera mainCamera;

  @ModifyVariable(method = "renderLevel", at = @At("STORE"), ordinal = 1)
  private Matrix4f injected(Matrix4f matrix4f2) {
    Quaternionf quat = mainCamera.rotation();
    return new Matrix4f().rotateY((float) Math.PI).rotate(quat.conjugate(new Quaternionf()));
  }
}
*//*? } else {*/
/*import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.leawind.perspectiveapi.api.PerspectiveHelper;
import net.minecraft.client.Camera;
import org.joml.Vector3f;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  @Shadow @Final private Camera mainCamera;

  @Inject(
      method = "renderLevel",
      at =
          @At(
              value = "INVOKE",
              target = "Lcom/mojang/math/Axis;rotationDegrees(F)Lorg/joml/Quaternionf;",
              ordinal = 2))
  public void doABarrelRoll$renderWorld(
      float tickDelta, long limitTime, PoseStack poseStack, CallbackInfo ci) {
    var quat = mainCamera.rotation();
    var eulerDeg = PerspectiveHelper.quatToEulerDeg(quat, new Vector3f());
    float roll = eulerDeg.z();
    if (roll != 0) {
      poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
    }
  }
}
*//*? }*/
