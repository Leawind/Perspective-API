package io.github.leawind.perspectiveapi.internal.logic.builtin.perspectives;

import com.google.auto.service.AutoService;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;

@SuppressWarnings("unused")
@AutoService(PerspectiveBehavior.class)
@PerspectiveInfo.Declaration(
    id = ThirdPersonFrontPerspective.ID,
    baseType = BaseType.THIRD_PERSON_FRONT,
    order = 2,
    icon = "perspective_api:textures/perspective/third_person_front.png",
    traits = {"third_person", "controllable", "switchable"})
public class ThirdPersonFrontPerspective implements PerspectiveBehavior {
  public static final String ID = "perspective_api.third_person_front";
}
