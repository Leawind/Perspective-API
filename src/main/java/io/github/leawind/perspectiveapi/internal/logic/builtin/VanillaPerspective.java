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
      new VanillaPerspective("first_person", CameraType.FIRST_PERSON);
  public static final VanillaPerspective THIRD_PERSON_BACK =
      new VanillaPerspective("third_person_back", CameraType.THIRD_PERSON_BACK);
  public static final VanillaPerspective THIRD_PERSON_FRONT =
      new VanillaPerspective("third_person_front", CameraType.THIRD_PERSON_FRONT);

  private final Identifier id;
  private final CameraType cameraType;
  private final Identifier icon;

  private VanillaPerspective(String name, CameraType cameraType) {
    this(Bridge.createIdentifier("minecraft", name), cameraType);
  }

  protected VanillaPerspective(Identifier id, CameraType cameraType) {
    this.id = id;
    this.cameraType = cameraType;
    this.icon = Bridge.createIdentifier(ICON_NAMESPACE, ICON_DIR + id.getPath() + ICON_SUFFIX);
  }

  @Override
  public @Nullable Identifier icon() {
    return icon;
  }

  @Override
  public final @NonNull Identifier id() {
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
