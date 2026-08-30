package io.github.leawind.perspectiveapi.internal.logic.builtin.perspectives;

import com.google.auto.service.AutoService;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
@AutoService(PerspectiveBehavior.class)
@PerspectiveInfo.Declaration(
    id = ThirdPersonBackPerspective.ID,
    order = 1,
    icon = "perspective_api:textures/perspective/third_person_back.png",
    traits = {"third_person", "controllable", "switchable"})
public class ThirdPersonBackPerspective implements PerspectiveBehavior {
  public static final String ID = "perspective_api.third_person_back";

  @Override
  public @NonNull BaseType getBaseType() {
    return BaseType.THIRD_PERSON_BACK;
  }
}
