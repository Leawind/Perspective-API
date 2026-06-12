package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.jspecify.annotations.NonNull;

public abstract class AbstractPerspective implements Perspective {
  protected final Vector3d position = new Vector3d();
  protected final Quaternionf rotation = new Quaternionf();

  @Override
  public final @NonNull Vector3dc getPosition() {
    return position;
  }

  @Override
  public final @NonNull Quaternionfc getRotation() {
    return rotation;
  }

  @Override
  public String toString() {
    // example:third_person{ExamplePerspective}
    return id() + "{" + getClass().getSimpleName() + "}";
  }
}
