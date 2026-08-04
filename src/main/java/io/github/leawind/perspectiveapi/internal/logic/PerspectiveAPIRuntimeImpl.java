package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherManager;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class PerspectiveAPIRuntimeImpl implements PerspectiveAPI.Runtime {
  static final PerspectiveAPIRuntimeImpl INSTANCE = new PerspectiveAPIRuntimeImpl();

  private PerspectiveAPIRuntimeImpl() {}

  @Override
  public @NonNull PerspectiveRegistry registry() {
    return PerspectiveRegistryImpl.INSTANCE;
  }

  @Override
  public @NonNull Transition transition() {
    return PerspectiveManager.INSTANCE.transition();
  }

  @Override
  public @NonNull PerspectiveModifierChain modifiers() {
    return PerspectiveManager.INSTANCE.modifiers();
  }

  @Override
  public @NonNull PerspectiveOverrideChain overrides() {
    return PerspectiveManager.INSTANCE.overrides();
  }

  @Override
  public @NonNull PerspectiveSwitcherManager switchers() {
    return PerspectiveManager.INSTANCE.switchers();
  }

  @Override
  public @Nullable Perspective current() {
    return PerspectiveManager.INSTANCE.getCurrent();
  }

  @Override
  public boolean isCurrent(@NonNull String id) {
    Objects.requireNonNull(id);
    Perspective current = PerspectiveManager.INSTANCE.getCurrent();
    return current != null && current.info().id().equals(id);
  }

  @Override
  public void onEnabledChanged(boolean enabled) {
    PerspectiveManager.INSTANCE.onEnabledChanged(enabled);
  }
}
