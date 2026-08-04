package io.github.leawind.perspectiveapi.internal.logic.builtin.perspectives;

import com.google.auto.service.AutoService;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;

@SuppressWarnings("unused")
@AutoService(PerspectiveBehavior.class)
@PerspectiveInfo.Default
@PerspectiveInfo.Declaration(
    id = FirstPersonPerspective.ID,
    baseType = BaseType.FIRST_PERSON,
    priority = 0,
    icon = "perspective_api:textures/perspective/first_person.png",
    traits = {"first_person", "controllable"})
public class FirstPersonPerspective implements PerspectiveBehavior {
  public static final String ID = "perspective_api.first_person";
}
