package io.github.leawind.perspectiveapi.internal.logic.builtin;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class VanillaPerspective implements Perspective {
  private static final String ICON_NAMESPACE = "perspective_api";
  private static final String ICON_DIR = "textures/perspective/";
  private static final String ICON_SUFFIX = ".png";

  public static final VanillaPerspective FIRST_PERSON =
      new VanillaPerspective("minecraft.first_person", CameraType.FIRST_PERSON, 0);
  public static final VanillaPerspective THIRD_PERSON_BACK =
      new VanillaPerspective("minecraft.third_person_back", CameraType.THIRD_PERSON_BACK, 1);
  public static final VanillaPerspective THIRD_PERSON_FRONT =
      new VanillaPerspective("minecraft.third_person_front", CameraType.THIRD_PERSON_FRONT, 2);

  private final String id;
  private final CameraType cameraType;
  private final Identifier icon;
  private final int priority;

  protected VanillaPerspective(String id, CameraType cameraType, int priority) {
    this.id = id;
    this.cameraType = cameraType;
    this.icon = Bridge.createIdentifier(ICON_NAMESPACE, ICON_DIR + id + ICON_SUFFIX);
    this.priority = priority;
  }

  @Override
  public @Nullable Identifier icon() {
    return icon;
  }

  @Override
  public int priority() {
    return priority;
  }

  @Override
  public final @NonNull String id() {
    return id;
  }

  @Override
  public final @NonNull CameraType cameraType() {
    return cameraType;
  }

  @Override
  public String toString() {
    return id() + "{" + getClass().getSimpleName() + "}";
  }
}
