package io.github.leawind.perspectiveapi.internal.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class PerspectiveSwitcherManagerImplTest {
  private static final class TestSwitcher implements PerspectiveSwitcherBehavior {
    private final String id;

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
    public void onSwitchablePerspectivesUpdated(
        @NonNull List<@NonNull Perspective> switchablePerspectives) {}

    @Override
    public void onActivated(@NonNull Perspective currentPerspective) {}

    @Override
    public void clientTickWhenActive(@NonNull Minecraft minecraft) {}

    @Override
    public void onDeactivated() {}

    @Override
    public @Nullable String getSelectedPerspectiveId() {
      return null;
    }
  }

  @Test
  void availableSwitchersAreSortedByStableId() {
    TestSwitcher defaultSwitcher = new TestSwitcher("test.z");
    TestSwitcher a = new TestSwitcher("test.a");
    TestSwitcher m = new TestSwitcher("test.m");
    PerspectiveSwitcherManagerImpl manager = new PerspectiveSwitcherManagerImpl(defaultSwitcher);
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
    PerspectiveSwitcherManagerImpl manager = new PerspectiveSwitcherManagerImpl(original);

    assertThrows(
        IllegalArgumentException.class, () -> manager.register(new TestSwitcher("test.same")));
    assertThrows(IllegalArgumentException.class, () -> manager.register(new TestSwitcher("")));
    assertSame(original, manager.getById("test.same"));
  }
}
