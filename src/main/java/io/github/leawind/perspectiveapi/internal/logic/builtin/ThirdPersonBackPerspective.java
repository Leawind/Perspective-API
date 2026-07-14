package io.github.leawind.perspectiveapi.internal.logic.builtin;

import io.github.leawind.perspectiveapi.api.Perspective;
import net.minecraft.client.CameraType;

@Perspective.Meta(
    id = ThirdPersonBackPerspective.ID,
    cameraType = CameraType.THIRD_PERSON_BACK,
    priority = 1,
    icon = "perspective_api:textures/perspective/perspective_api.third_person_back.png")
public class ThirdPersonBackPerspective implements Perspective {
  public static final String ID = "perspective_api.third_person_back";
}
