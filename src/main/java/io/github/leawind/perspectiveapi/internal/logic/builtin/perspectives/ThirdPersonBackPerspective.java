package io.github.leawind.perspectiveapi.internal.logic.builtin.perspectives;

import com.google.auto.service.AutoService;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.Info;

@SuppressWarnings("unused")
@AutoService(PerspectiveBehavior.class)
@Info(
    id = ThirdPersonBackPerspective.ID,
    baseType = BaseType.THIRD_PERSON_BACK,
    priority = 1,
    icon = "perspective_api:textures/perspective/third_person_back.png")
public class ThirdPersonBackPerspective implements PerspectiveBehavior {
  public static final String ID = "perspective_api.third_person_back";
}
