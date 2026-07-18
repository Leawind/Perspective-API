package io.github.leawind.perspectiveapi.internal.logic.builtin.perspectives;

import com.google.auto.service.AutoService;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.*;
import net.minecraft.client.CameraType;

@SuppressWarnings("unused")
@AutoService(PerspectiveBehavior.class)
@Default
@Info(
    id = FirstPersonPerspective.ID,
    cameraType = CameraType.FIRST_PERSON,
    priority = 0,
    icon = "perspective_api:textures/perspective/first_person.png")
public class FirstPersonPerspective implements PerspectiveBehavior {
  public static final String ID = "perspective_api.first_person";
}
