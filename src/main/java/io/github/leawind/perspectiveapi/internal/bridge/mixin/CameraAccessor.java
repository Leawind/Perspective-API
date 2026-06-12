package io.github.leawind.perspectiveapi.internal.bridge.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
  @Invoker("setPosition")
  void invokeSetPosition(double x, double y, double z);

  @Accessor("rotation")
  Quaternionf getRotation();

  @Accessor("position")
  Vec3 getPosition();

  @Accessor("forwards")
  Vector3f getForwards();

  @Accessor("up")
  Vector3f getUp();

  @Accessor("left")
  Vector3f getLeft();

  @Accessor("xRot")
  void setXRot(float xRot);

  @Accessor("yRot")
  void setYRot(float yRot);

  @Accessor("entity")
  Entity getEntity();
}
