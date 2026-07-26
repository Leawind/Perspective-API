package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import java.util.ServiceConfigurationError;
import org.junit.jupiter.api.Test;

class PerspectiveRegistryImplTest {

  private static final String PRIORITY_ID = "test.registry_duplicate_priority";
  private static final String CLASS_NAME_ID = "test.registry_duplicate_class_name";
  private static final String AVAILABILITY_ID = "test.registry_availability";

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

  @PerspectiveBehavior.Info(id = AVAILABILITY_ID)
  private static final class ToggleAvailabilityPerspective implements PerspectiveBehavior {
    private boolean available;
    private boolean throwsException;
    private int evaluationCount;

    @Override
    public boolean isAvailable() {
      evaluationCount++;
      if (throwsException) throw new IllegalStateException("availability failure");
      return available;
    }
  }

  @Test
  void rejectEmptyId() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();

    assertThrows(
        ServiceConfigurationError.class, () -> registry.registerSilent(new EmptyIdPerspective()));
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

  @Test
  void reuseAvailabilityResultWithinClientTick() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    ToggleAvailabilityPerspective behavior = new ToggleAvailabilityPerspective();
    registry.registerSilent(behavior);
    var perspective = registry.get(AVAILABILITY_ID);
    var overrides = new PerspectiveOverrideChainImpl(registry);
    overrides.push("test.override", 0, () -> AVAILABILITY_ID);

    behavior.available = true;
    registry.beginAvailabilitySnapshot();

    assertTrue(perspective.isAvailable());
    behavior.available = false;
    assertEquals(AVAILABILITY_ID, overrides.get());
    assertTrue(perspective.isAvailable());
    assertEquals(1, behavior.evaluationCount);

    registry.beginAvailabilitySnapshot();

    assertFalse(perspective.isAvailable());
    assertEquals(2, behavior.evaluationCount);
  }

  @Test
  void cacheAvailabilityFailureWithinClientTick() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    ToggleAvailabilityPerspective behavior = new ToggleAvailabilityPerspective();
    registry.registerSilent(behavior);
    var perspective = registry.get(AVAILABILITY_ID);

    behavior.throwsException = true;
    registry.beginAvailabilitySnapshot();

    assertFalse(perspective.isAvailable());
    behavior.throwsException = false;
    behavior.available = true;
    assertFalse(perspective.isAvailable());
    assertEquals(1, behavior.evaluationCount);

    registry.beginAvailabilitySnapshot();

    assertTrue(perspective.isAvailable());
    assertEquals(2, behavior.evaluationCount);
  }
}
