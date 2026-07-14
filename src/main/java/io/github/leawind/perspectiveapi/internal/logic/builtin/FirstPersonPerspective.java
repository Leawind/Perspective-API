package io.github.leawind.perspectiveapi.internal.logic.builtin;

import io.github.leawind.perspectiveapi.api.Perspective;
import net.minecraft.client.CameraType;

@Perspective.Default
@Perspective.Meta(
    id = FirstPersonPerspective.ID,
    cameraType = CameraType.FIRST_PERSON,
    priority = 0,
    icon = "perspective_api:textures/perspective/perspective_api.first_person.png")
public class FirstPersonPerspective implements Perspective {
  public static final String ID = "perspective_api.first_person";
}
