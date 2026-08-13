package io.github.leawind.perspectiveapi.internal.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class PerspectiveSwitcherManagerImplTest {
  private static final class TestSwitcher implements PerspectiveSwitcherBehavior {
    private final String id;
    private int initCalls;
    private int activationCalls;
    private int deactivationCalls;

    private TestSwitcher(String id) {
      this.id = id;
    }

    @Override
    public @NonNull String id() {
      return id;
    }

    @Override
    public @NonNull Component name() {
      return Component.literal(id);
    }

    @Override
    public void init() {
      initCalls++;
    }

    @Override
    public void onSwitchablePerspectivesUpdated(
        @NonNull List<@NonNull Perspective> switchablePerspectives) {}

    @Override
    public void onActivated() {
      activationCalls++;
    }

    @Override
    public void onDeactivated() {
      deactivationCalls++;
    }
  }

  @Test
  void availableSwitchersAreSortedByStableId() {
    TestSwitcher defaultSwitcher = new TestSwitcher("test.z");
    TestSwitcher a = new TestSwitcher("test.a");
    TestSwitcher m = new TestSwitcher("test.m");
    PerspectiveSwitcherManagerImpl manager = manager(defaultSwitcher, new PerspectiveRegistryImpl());
    manager.register(m);
    manager.register(a);

    assertEquals(
        List.of("test.a", "test.m", "test.z"),
        manager.getAvailableSwitchers().stream().map(switcher -> switcher.id()).toList());
    assertSame(a, manager.getById("test.a"));
  }

  @Test
  void rejectsDuplicateAndEmptyIds() {
    TestSwitcher original = new TestSwitcher("test.same");
    PerspectiveSwitcherManagerImpl manager = manager(original, new PerspectiveRegistryImpl());

    assertThrows(
        IllegalArgumentException.class, () -> manager.register(new TestSwitcher("test.same")));
    assertThrows(IllegalArgumentException.class, () -> manager.register(new TestSwitcher("")));
    assertSame(original, manager.getById("test.same"));
  }

  @Test
  void initializesSwitchersAndManagesTheirLifecycle() {
    TestSwitcher a = new TestSwitcher("test.a");
    TestSwitcher b = new TestSwitcher("test.b");
    PerspectiveSwitcherManagerImpl manager = manager(a, new PerspectiveRegistryImpl());
    manager.register(b);

    assertEquals(1, a.initCalls);
    assertEquals(1, b.initCalls);
    assertSame(a, manager.getSelectedSwitcher());
    assertEquals(1, a.activationCalls);
    assertEquals(0, a.deactivationCalls);
    assertEquals(0, b.activationCalls);

    manager.setSelectedSwitcher(b);
    assertEquals(1, a.deactivationCalls);
    assertEquals(1, b.activationCalls);
  }

  private static PerspectiveSwitcherManagerImpl manager(
      TestSwitcher defaultSwitcher, PerspectiveRegistryImpl registry) {
    return new PerspectiveSwitcherManagerImpl(defaultSwitcher, registry);
  }
}
