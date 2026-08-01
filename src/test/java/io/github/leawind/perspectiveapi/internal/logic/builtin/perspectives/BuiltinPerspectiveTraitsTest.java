package io.github.leawind.perspectiveapi.internal.logic.builtin.perspectives;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import org.junit.jupiter.api.Test;

class BuiltinPerspectiveTraitsTest {

  @Test
  void declareFirstPersonTraitOnlyForFirstPerson() {
    PerspectiveInfo info = infoOf(FirstPersonPerspective.class);

    assertTrue(info.declaresTrait("first_person"));
    assertFalse(info.declaresTrait("third_person"));
  }

  @Test
  void declareThirdPersonTraitForBothThirdPersonPerspectives() {
    PerspectiveInfo backInfo = infoOf(ThirdPersonBackPerspective.class);
    PerspectiveInfo frontInfo = infoOf(ThirdPersonFrontPerspective.class);

    assertTrue(backInfo.declaresTrait("third_person"));
    assertTrue(frontInfo.declaresTrait("third_person"));
    assertFalse(backInfo.declaresTrait("first_person"));
    assertFalse(frontInfo.declaresTrait("first_person"));
  }

  private static PerspectiveInfo infoOf(
      Class<? extends PerspectiveBehavior> perspectiveBehaviorClass) {
    PerspectiveInfo.Declaration declaration =
        perspectiveBehaviorClass.getAnnotation(PerspectiveInfo.Declaration.class);
    return PerspectiveInfo.fromDeclaration(declaration);
  }
}
