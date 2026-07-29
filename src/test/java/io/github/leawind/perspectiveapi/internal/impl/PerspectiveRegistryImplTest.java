package io.github.leawind.perspectiveapi.internal.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior;
import io.github.leawind.perspectiveapi.api.PerspectiveBehavior.BaseType;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PerspectiveRegistryImplTest {

  private static final String PRIORITY_ID = "test.registry_duplicate_priority";
  private static final String CLASS_NAME_ID = "test.registry_duplicate_class_name";
  private static final String AVAILABILITY_ID = "test.registry_availability";
  private static final String ROLLBACK_ID = "test.registry_rollback";

  private static final class MissingInfoPerspective implements PerspectiveBehavior {}

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

  @PerspectiveBehavior.Info(
      id = "test.registry_metadata",
      baseType = BaseType.THIRD_PERSON_FRONT,
      nameKey = "test.registry.name",
      descriptionKey = "test.registry.description",
      switchable = false,
      priority = 7)
  private static final class MetadataPerspective implements PerspectiveBehavior {}

  @PerspectiveBehavior.Info(id = "test.registry_default_low", priority = 20)
  @PerspectiveBehavior.Default(priority = 1)
  private static final class LowDefaultPerspective implements PerspectiveBehavior {}

  @PerspectiveBehavior.Info(id = "test.registry_default_b", priority = 10)
  @PerspectiveBehavior.Default(priority = 5)
  private static final class DefaultBPerspective implements PerspectiveBehavior {}

  @PerspectiveBehavior.Info(id = "test.registry_default_a", priority = 10)
  @PerspectiveBehavior.Default(priority = 5)
  private static final class DefaultAPerspective implements PerspectiveBehavior {}

  @PerspectiveBehavior.Info(id = ROLLBACK_ID, priority = 10)
  private static final class OriginalPerspective implements PerspectiveBehavior {
    private final AtomicInteger initCalls;

    private OriginalPerspective(AtomicInteger initCalls) {
      this.initCalls = initCalls;
    }

    @Override
    public void init() {
      initCalls.incrementAndGet();
    }
  }

  @PerspectiveBehavior.Info(id = ROLLBACK_ID, priority = 0)
  private static final class FailingReplacementPerspective implements PerspectiveBehavior {
    @Override
    public void init() {
      throw new IllegalStateException("init failure");
    }
  }

  @Test
  void rejectEmptyId() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();

    assertThrows(
        ServiceConfigurationError.class, () -> registry.registerSilent(new EmptyIdPerspective()));
  }

  @Test
  void rejectMissingInfoAnnotationAndNullBehavior() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();

    assertThrows(
        ServiceConfigurationError.class,
        () -> registry.registerSilent(new MissingInfoPerspective()));
    assertThrows(NullPointerException.class, () -> registry.registerSilent(null));
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
  void exposeAnnotationMetadata() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    MetadataPerspective behavior = new MetadataPerspective();
    registry.registerSilent(behavior);

    Perspective perspective = registry.get("test.registry_metadata");

    assertEquals("test.registry_metadata", perspective.id());
    assertEquals(BaseType.THIRD_PERSON_FRONT, perspective.baseType());
    assertFalse(perspective.switchable());
    assertEquals(7, perspective.priority());
    assertEquals("test.registry.name", perspective.name().getString());
    assertEquals("test.registry.description", perspective.description().getString());
    assertNull(perspective.icon());
    assertSame(behavior, registry.getBehaviorOrThrow(perspective.id()));
  }

  @Test
  void getAllSortsByPriorityThenId() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    registry.registerSilent(new LowDefaultPerspective());
    registry.registerSilent(new DefaultBPerspective());
    registry.registerSilent(new DefaultAPerspective());

    assertEquals(
        List.of("test.registry_default_a", "test.registry_default_b", "test.registry_default_low"),
        registry.getAllPerspectives().stream().map(Perspective::id).toList());
    assertFalse(registry.contains(null));
    assertTrue(registry.contains("test.registry_default_a"));
  }

  @Test
  void resolveDefaultByHighestDefaultPriorityThenId() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    LowDefaultPerspective low = new LowDefaultPerspective();
    DefaultBPerspective highB = new DefaultBPerspective();
    DefaultAPerspective highA = new DefaultAPerspective();
    registry.registerSilent(low);
    registry.registerSilent(highB);
    registry.registerSilent(highA);

    assertTrue(registry.isDefaultFound());
    assertEquals("test.registry_default_a", registry.getDefault().id());
    assertSame(highA, registry.getDefaultBehavior());
    assertSame(registry.getDefault(), registry.getOrDefault(null));
    assertSame(registry.getDefault(), registry.getOrDefault("test.missing"));
    assertSame(highA, registry.getBehaviorOrDefault("test.missing"));
  }

  @Test
  void gettersFailClearlyWhenEntryOrDefaultIsMissing() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();

    assertNull(registry.get("test.missing"));
    assertThrows(IllegalArgumentException.class, () -> registry.getOrThrow("test.missing"));
    assertThrows(IllegalArgumentException.class, () -> registry.getBehaviorOrThrow("test.missing"));
    assertThrows(IllegalStateException.class, registry::getDefault);
    assertThrows(IllegalStateException.class, () -> registry.getOrDefault(null));
  }

  @Test
  void duplicateSameBehaviorIsIgnoredWithoutReinitializing() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    AtomicInteger initCalls = new AtomicInteger();
    OriginalPerspective behavior = new OriginalPerspective(initCalls);

    registry.registerSilent(behavior);
    registry.registerSilent(behavior);

    assertEquals(1, initCalls.get());
  }

  @Test
  void failedReplacementRestoresExistingRegistration() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    OriginalPerspective original = new OriginalPerspective(new AtomicInteger());
    registry.registerSilent(original);

    assertThrows(
        IllegalStateException.class,
        () -> registry.registerSilent(new FailingReplacementPerspective()));

    assertSame(original, registry.getBehaviorOrThrow(ROLLBACK_ID));
  }

  @Test
  void reuseAvailabilityResultWithinClientTick() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    ToggleAvailabilityPerspective behavior = new ToggleAvailabilityPerspective();
    registry.registerSilent(behavior);
    var perspective = registry.get(AVAILABILITY_ID);
    var overrides = new PerspectiveOverrideChainImpl(registry);
    overrides.register("test.override", 0, () -> AVAILABILITY_ID);

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
