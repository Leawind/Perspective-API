package io.github.leawind.perspectiveapi.internal.logic.builtin;

import io.github.leawind.perspectiveapi.api.Perspective;
import net.minecraft.client.CameraType;

@Perspective.Meta(
    id = ThirdPersonFrontPerspective.ID,
    cameraType = CameraType.THIRD_PERSON_FRONT,
    priority = 2,
    icon = "perspective_api:textures/perspective/perspective_api.third_person_front.png")
public class ThirdPersonFrontPerspective implements Perspective {
  public static final String ID = "perspective_api.third_person_front";
}
