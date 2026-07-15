package io.github.leawind.perspectiveapi.internal.logic.builtin;

import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.*;
import net.minecraft.client.CameraType;

@Default
@Meta(
    id = FirstPersonPerspective.ID,
    cameraType = CameraType.FIRST_PERSON,
    priority = 0,
    icon = "perspective_api:textures/perspective/perspective_api.first_person.png")
public class FirstPersonPerspective implements PerspectiveBehavior {
  public static final String ID = "perspective_api.first_person";
}
