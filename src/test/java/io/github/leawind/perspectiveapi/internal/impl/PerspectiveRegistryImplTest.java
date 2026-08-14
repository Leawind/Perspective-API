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
import io.github.leawind.perspectiveapi.api.PerspectiveInfo;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class PerspectiveRegistryImplTest {

  private static final String ORDER_ID = "test.registry_duplicate_order";
  private static final String CLASS_NAME_ID = "test.registry_duplicate_class_name";
  private static final String AVAILABILITY_ID = "test.registry_availability";
  private static final String ROLLBACK_ID = "test.registry_rollback";

  private static final class MissingInfoPerspective implements PerspectiveBehavior {}

  private static final class EqualPerspective implements PerspectiveBehavior {
    @Override
    public boolean equals(Object obj) {
      return obj instanceof EqualPerspective;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  @PerspectiveInfo.Declaration(id = "", order = 0)
  private static final class EmptyIdPerspective implements PerspectiveBehavior {}

  @PerspectiveInfo.Declaration(id = ORDER_ID, order = 10)
  private static final class EarlierPerspective implements PerspectiveBehavior {}

  @PerspectiveInfo.Declaration(id = ORDER_ID, order = 20)
  private static final class LaterPerspective implements PerspectiveBehavior {}

  @PerspectiveInfo.Declaration(id = CLASS_NAME_ID, baseType = BaseType.FIRST_PERSON, order = 0)
  private static final class AlphaPerspective implements PerspectiveBehavior {}

  @PerspectiveInfo.Declaration(
      id = CLASS_NAME_ID,
      baseType = BaseType.THIRD_PERSON_BACK,
      order = 0)
  private static final class BetaPerspective implements PerspectiveBehavior {}

  @PerspectiveInfo.Declaration(id = AVAILABILITY_ID)
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

  @PerspectiveInfo.Declaration(
      id = "test.registry_metadata",
      baseType = BaseType.THIRD_PERSON_FRONT,
      nameKey = "test.registry.name",
      descriptionKey = "test.registry.description",
      order = 7,
      traits = {"third_person", "switchable", "test:custom_trait"})
  private static final class MetadataPerspective implements PerspectiveBehavior {}

  @PerspectiveInfo.Declaration(id = "test.registry_default_low", order = 20)
  @PerspectiveInfo.Default(priority = 1)
  private static final class LowDefaultPerspective implements PerspectiveBehavior {}

  @PerspectiveInfo.Declaration(id = "test.registry_default_b", order = 10)
  @PerspectiveInfo.Default(priority = 5)
  private static final class DefaultBPerspective implements PerspectiveBehavior {}

  @PerspectiveInfo.Declaration(id = "test.registry_default_a", order = 10)
  @PerspectiveInfo.Default(priority = 5)
  private static final class DefaultAPerspective implements PerspectiveBehavior {}

  @PerspectiveInfo.Declaration(id = ROLLBACK_ID, order = 10)
  private static final class OriginalPerspective implements PerspectiveBehavior {
    private final AtomicInteger initCalls;

    private OriginalPerspective(AtomicInteger initCalls) {
      this.initCalls = initCalls;
    }

    @Override
    public void initialize() {
      initCalls.incrementAndGet();
    }
  }

  @PerspectiveInfo.Declaration(id = ROLLBACK_ID, order = 0)
  private static final class FailingReplacementPerspective implements PerspectiveBehavior {
    @Override
    public void initialize() {
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
  void keepEarlierDuplicate() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    registry.registerSilent(new LaterPerspective());
    registry.registerSilent(new EarlierPerspective());

    assertEquals(10, registry.get(ORDER_ID).info().order());
  }

  @Test
  void resolveEqualOrderByBehaviorClassName() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    registry.registerSilent(new BetaPerspective());
    registry.registerSilent(new AlphaPerspective());

    assertEquals(BaseType.FIRST_PERSON, registry.get(CLASS_NAME_ID).info().baseType());
  }

  @Test
  void exposeAnnotationMetadata() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    MetadataPerspective behavior = new MetadataPerspective();
    registry.registerSilent(behavior);

    Perspective perspective = registry.get("test.registry_metadata");

    PerspectiveInfo info = perspective.info();
    assertEquals("test.registry_metadata", info.id());
    assertEquals(BaseType.THIRD_PERSON_FRONT, info.baseType());
    assertEquals(7, info.order());
    assertEquals("test.registry.name", info.name().getString());
    assertEquals("test.registry.description", info.description().getString());
    assertNull(info.icon());
    assertEquals(Set.of("third_person", "switchable", "test:custom_trait"), info.traits());
    assertTrue(info.hasTrait("third_person"));
    assertTrue(info.hasTrait("switchable"));
    assertFalse(info.hasTrait("orthographic"));
    assertSame(behavior, registry.getBehaviorOrThrow(info.id()));
  }

  @Test
  void validateAndCopyTraits() {
    List<String> source = new ArrayList<>(List.of("third_person"));
    PerspectiveInfo info =
        PerspectiveInfo.builder("test.traits", Component.literal("Traits"))
            .traits(source)
            .trait("third_person")
            .trait("test:custom_trait")
            .build();

    source.clear();

    assertEquals(Set.of("third_person", "test:custom_trait"), info.traits());
    assertFalse(info.hasTrait("switchable"));
    assertThrows(UnsupportedOperationException.class, () -> info.traits().add("orthographic"));
    assertThrows(NullPointerException.class, () -> info.hasTrait(null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            PerspectiveInfo.builder("test.invalid", Component.literal("Invalid"))
                .trait("Third Person"));
  }

  @Test
  void getAllSortsByOrderThenId() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    registry.registerSilent(new LowDefaultPerspective());
    registry.registerSilent(new DefaultBPerspective());
    registry.registerSilent(new DefaultAPerspective());

    assertEquals(
        List.of("test.registry_default_a", "test.registry_default_b", "test.registry_default_low"),
        registry.getAllPerspectives().stream()
            .map(perspective -> perspective.info().id())
            .toList());
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
    assertEquals("test.registry_default_a", registry.getDefault().info().id());
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
  void rejectDuplicateBehaviorInstanceWithoutReinitializing() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    AtomicInteger initCalls = new AtomicInteger();
    OriginalPerspective behavior = new OriginalPerspective(initCalls);

    registry.registerSilent(behavior);
    assertThrows(IllegalArgumentException.class, () -> registry.registerSilent(behavior));

    assertEquals(1, initCalls.get());
  }

  @Test
  void registerRuntimePerspective() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    PerspectiveBehavior behavior = new MissingInfoPerspective();
    AtomicInteger updates = new AtomicInteger();
    registry.onUpdate().on(updates::incrementAndGet);
    PerspectiveInfo initial =
        PerspectiveInfo.builder("test.runtime", Component.literal("Runtime"))
            .baseType(BaseType.FIRST_PERSON)
            .order(3)
            .trait("first_person")
            .build();

    PerspectiveRegistration registration = registry.register(initial, behavior);
    Perspective perspective = registration.perspective();

    assertSame(perspective, registry.get("test.runtime"));
    assertSame(initial, perspective.info());
    assertEquals("Runtime", perspective.info().name().getString());
    assertEquals(BaseType.FIRST_PERSON, perspective.info().baseType());
    assertTrue(perspective.info().hasTrait("first_person"));
    assertEquals(1, updates.get());

    assertTrue(registration.unregister());
    assertFalse(registration.unregister());
    assertEquals(2, updates.get());
  }

  @Test
  void rejectDuplicateRuntimeIdAndBehaviorInstance() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    PerspectiveBehavior behavior = new MissingInfoPerspective();
    registry.register(runtimeInfo("test.runtime_a"), behavior);

    assertThrows(
        IllegalArgumentException.class,
        () -> registry.register(runtimeInfo("test.runtime_b"), behavior));
    assertThrows(
        IllegalArgumentException.class,
        () -> registry.register(runtimeInfo("test.runtime_a"), new MissingInfoPerspective()));
  }

  @Test
  void compareBehaviorInstancesByIdentityAndAllowReuseAfterRemoval() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    EqualPerspective firstBehavior = new EqualPerspective();
    EqualPerspective equalButDistinctBehavior = new EqualPerspective();
    PerspectiveRegistration first =
        registry.register(runtimeInfo("test.identity_first"), firstBehavior);

    registry.register(runtimeInfo("test.identity_second"), equalButDistinctBehavior);
    assertTrue(first.unregister());
    registry.register(runtimeInfo("test.identity_reused"), firstBehavior);
  }

  @Test
  void oldHandleCannotRemoveNewRegistrationWithReusedId() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    PerspectiveRegistration oldRegistration =
        registry.register(runtimeInfo("test.reused"), new MissingInfoPerspective());
    assertTrue(oldRegistration.unregister());

    PerspectiveRegistration newRegistration =
        registry.register(runtimeInfo("test.reused"), new MissingInfoPerspective());

    assertFalse(oldRegistration.unregister());
    assertSame(newRegistration.perspective(), registry.get("test.reused"));
  }

  @Test
  void preventRemovingLastDefaultPerspective() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    PerspectiveRegistration first =
        registry.registerDefault(
            runtimeInfo("test.default_first"), 10, new MissingInfoPerspective());

    assertThrows(IllegalStateException.class, first::unregister);
    assertSame(first.perspective(), registry.getDefault());

    PerspectiveRegistration second =
        registry.registerDefault(
            runtimeInfo("test.default_second"), 20, new MissingInfoPerspective());

    assertSame(second.perspective(), registry.getDefault());
    assertTrue(second.unregister());
    assertSame(first.perspective(), registry.getDefault());
    assertThrows(IllegalStateException.class, first::unregister);
  }

  @Test
  void initializingDefaultDoesNotPermitRemovingEstablishedLastDefault() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    PerspectiveRegistration established =
        registry.registerDefault(
            runtimeInfo("test.default_established"), 10, new MissingInfoPerspective());
    PerspectiveBehavior failing =
        new PerspectiveBehavior() {
          @Override
          public void initialize() {
            established.unregister();
            throw new IllegalStateException("init failure");
          }
        };

    assertThrows(
        IllegalStateException.class,
        () -> registry.registerDefault(runtimeInfo("test.default_initializing"), 20, failing));

    assertSame(established.perspective(), registry.getDefault());
    assertFalse(registry.contains("test.default_initializing"));
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
  void evaluatesAvailabilityForEveryCall() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    ToggleAvailabilityPerspective behavior = new ToggleAvailabilityPerspective();
    registry.registerSilent(behavior);
    var perspective = registry.get(AVAILABILITY_ID);
    var overrides = new PerspectiveOverrideChainImpl(registry);
    overrides.register(0, () -> AVAILABILITY_ID);

    behavior.available = true;
    assertTrue(perspective.isAvailable());
    behavior.available = false;
    assertNull(overrides.get());
    assertFalse(perspective.isAvailable());
    assertEquals(3, behavior.evaluationCount);
  }

  @Test
  void retriesAvailabilityAfterFailure() {
    PerspectiveRegistryImpl registry = new PerspectiveRegistryImpl();
    ToggleAvailabilityPerspective behavior = new ToggleAvailabilityPerspective();
    registry.registerSilent(behavior);
    var perspective = registry.get(AVAILABILITY_ID);

    behavior.throwsException = true;
    assertFalse(perspective.isAvailable());
    behavior.throwsException = false;
    behavior.available = true;
    assertTrue(perspective.isAvailable());
    assertEquals(2, behavior.evaluationCount);
  }

  private static PerspectiveInfo runtimeInfo(String id) {
    return PerspectiveInfo.builder(id, Component.literal(id)).build();
  }
}
