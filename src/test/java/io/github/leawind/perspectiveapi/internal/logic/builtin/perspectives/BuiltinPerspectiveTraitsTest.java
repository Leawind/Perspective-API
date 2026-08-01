package io.github.leawind.perspectiveapi.internal.logic.builtin.perspectives;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuiltinPerspectiveTraitsTest {

  @Test
  void declareFirstPersonTraitOnlyForFirstPerson() {
    List<String> traits = traitsOf(FirstPersonPerspective.class);

    assertTrue(traits.contains("first_person"));
    assertFalse(traits.contains("third_person"));
  }

  @Test
  void declareThirdPersonTraitForBothThirdPersonPerspectives() {
    List<String> backTraits = traitsOf(ThirdPersonBackPerspective.class);
    List<String> frontTraits = traitsOf(ThirdPersonFrontPerspective.class);

    assertTrue(backTraits.contains("third_person"));
    assertTrue(frontTraits.contains("third_person"));
    assertFalse(backTraits.contains("first_person"));
    assertFalse(frontTraits.contains("first_person"));
  }

  private static List<String> traitsOf(
      Class<? extends PerspectiveBehavior> perspectiveBehaviorClass) {
    PerspectiveInfo.Declaration declaration =
        perspectiveBehaviorClass.getAnnotation(PerspectiveInfo.Declaration.class);
    return List.of(declaration.traits());
  }
}
