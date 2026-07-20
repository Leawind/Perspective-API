package io.github.leawind.perspectiveapi.internal.logic.builtin.perspectives;

import com.google.auto.service.AutoService;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.*;

@SuppressWarnings("unused")
@AutoService(PerspectiveBehavior.class)
@Info(
    id = ThirdPersonFrontPerspective.ID,
    baseType = BaseType.THIRD_PERSON_FRONT,
    priority = 2,
    icon = "perspective_api:textures/perspective/third_person_front.png")
public class ThirdPersonFrontPerspective implements PerspectiveBehavior {
  public static final String ID = "perspective_api.third_person_front";
}
