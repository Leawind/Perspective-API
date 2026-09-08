package io.github.leawind.perspectiveapi.internal.logic;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPIRuntime;
import io.github.leawind.perspectiveapi.api.PerspectiveChangeListener;
import io.github.leawind.perspectiveapi.api.PerspectiveChangeListenerRegistration;
import io.github.leawind.perspectiveapi.api.PerspectiveModifierChain;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.api.PerspectiveSelection;
import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class PerspectiveAPIRuntimeImpl implements PerspectiveAPIRuntime.Services {
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
  public @NonNull PerspectiveSelection selection() {
    return PerspectiveManager.INSTANCE.selection();
  }

  @Override
  public @Nullable Perspective current() {
    return PerspectiveManager.INSTANCE.getCurrent();
  }

  @Override
  public @Nullable PerspectiveState previousCameraState() {
    return PerspectiveManager.INSTANCE.getPreviousCameraState();
  }

  @Override
  public boolean isCurrent(@NonNull String id) {
    Objects.requireNonNull(id);
    Perspective current = PerspectiveManager.INSTANCE.getCurrent();
    return current != null && current.info().id().equals(id);
  }

  @Override
  public @NonNull PerspectiveChangeListenerRegistration onCurrentChanged(
      @NonNull PerspectiveChangeListener listener) {
    return PerspectiveManager.INSTANCE.onCurrentChanged(listener);
  }

  @Override
  public void onEnabledChanged(boolean enabled) {
    PerspectiveManager.INSTANCE.onEnabledChanged(enabled);
  }
}
