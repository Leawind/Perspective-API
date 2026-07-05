package io.github.leawind.perspectiveapi.internal.logic.builtin;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import net.minecraft.client.CameraType;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class VanillaPerspective implements Perspective {
  public static final VanillaPerspective FIRST_PERSON =
      new VanillaPerspective("first_person", CameraType.FIRST_PERSON);
  public static final VanillaPerspective THIRD_PERSON_BACK =
      new VanillaPerspective("third_person_back", CameraType.THIRD_PERSON_BACK);
  public static final VanillaPerspective THIRD_PERSON_FRONT =
      new VanillaPerspective("third_person_front", CameraType.THIRD_PERSON_FRONT);

  private final Identifier id;
  private final CameraType cameraType;

  private VanillaPerspective(String name, CameraType cameraType) {
    this(Bridge.createIdentifier("minecraft", name), cameraType);
  }

  protected VanillaPerspective(Identifier id, CameraType cameraType) {
    this.id = id;
    this.cameraType = cameraType;
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
