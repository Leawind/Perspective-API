package io.github.leawind.perspectiveapi.internal.bridge.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessorMixin {
  // Due to a bug of mixin, do not access instance field `forwards`, `up` or `left` here.
  // See `CameraMixin`, `CameraAccessor`

  @Invoker("setPosition")
  void invokeSetPosition(double x, double y, double z);

  @Invoker("setRotation")
  void invokeSetRotation(float yRot, float xRot);

  /// In 1.20.4, there is a method `getEntity()`, later it's renamed to `entity()`
  ///
  /// I'm lazy, so let's just access the field directly.
  @Accessor("entity")
  Entity getEntity();

  @Accessor("rotation")
  Quaternionf getRotation();

  @Accessor("position")
  Vec3 getPosition();

  @Accessor("xRot")
  void setXRot(float xRot);

  @Accessor("yRot")
  void setYRot(float yRot);

  /*? if >=26.1 {*/
  @Accessor("matrixPropertiesDirty")
  int getMatrixPropertiesDirty();

  @Accessor("matrixPropertiesDirty")
  void setMatrixPropertiesDirty(int dirty);
  /*? }*/

  /*? if >=26.1 {*/
  @Accessor("fov")
  float getFov();
  /*? }*/
}
