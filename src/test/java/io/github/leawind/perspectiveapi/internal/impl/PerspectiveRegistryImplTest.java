package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import java.util.ServiceConfigurationError;
import org.junit.jupiter.api.Test;

class PerspectiveRegistryImplTest {

  private static final String PRIORITY_ID = "test.registry_duplicate_priority";
  private static final String CLASS_NAME_ID = "test.registry_duplicate_class_name";

  @PerspectiveBehavior.Info(id = "", priority = 0)
  private static final class EmptyIdPerspective implements PerspectiveBehavior {}

  @PerspectiveBehavior.Info(id = PRIORITY_ID, priority = 10)
  private static final class LowerPriorityPerspective implements PerspectiveBehavior {}

  @PerspectiveBehavior.Info(id = PRIORITY_ID, priority = 20)
  private static final class HigherPriorityPerspective implements PerspectiveBehavior {}

  @PerspectiveBehavior.Info(id = CLASS_NAME_ID, baseType = BaseType.FIRST_PERSON, priority = 0)
  private static final class AlphaPerspective implements PerspectiveBehavior {}

  @PerspectiveBehavior.Info(id = CLASS_NAME_ID, baseType = BaseType.THIRD_PERSON_BACK, priority = 0)
  private static final class BetaPerspective implements PerspectiveBehavior {}

  @Test
  void rejectEmptyId() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();

    assertThrows(ServiceConfigurationError.class, () -> registry.registerSilent(new EmptyIdPerspective()));
  }

  @Test
  void keepLowerPriorityDuplicate() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    registry.registerSilent(new HigherPriorityPerspective());
    registry.registerSilent(new LowerPriorityPerspective());

    assertEquals(10, registry.get(PRIORITY_ID).priority());
  }

  @Test
  void resolveEqualPriorityByBehaviorClassName() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    registry.registerSilent(new BetaPerspective());
    registry.registerSilent(new AlphaPerspective());

    assertEquals(BaseType.FIRST_PERSON, registry.get(CLASS_NAME_ID).baseType());
  }
}
