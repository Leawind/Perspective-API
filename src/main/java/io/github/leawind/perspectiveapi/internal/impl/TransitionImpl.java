package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.PerspectiveState;
import io.github.leawind.perspectiveapi.api.Transition;
import io.github.leawind.perspectiveapi.internal.utils.PerspectiveUtils;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

public final class TransitionImpl implements Transition {

  private static final long MIN_DELTA_MS = 1;

  // region settings
  private long duration = 300;
  private Blender blender = Blender::easeOut;
  // endregion

  private long startTime;
  private final PerspectiveState.Mutable startState = new PerspectiveStateImpl();
  private final PerspectiveState.Mutable currentState = new PerspectiveStateImpl();

  TransitionImpl() {}

  @Override
  public boolean isInTransition(long now) {
    return now - startTime < duration;
  }

  @Override
  public void setDuration(long duration) {
    this.duration = duration;
  }

  @Override
  public void setBlender(@NonNull Blender blender) {
    this.blender = Objects.requireNonNull(blender);
  }

  @Override
  public @NonNull Blender getBlender() {
    return blender;
  }

  @Override
  public void start(double now, @NonNull PerspectiveState state) {
    Objects.requireNonNull(state);
    startTime = (long) now;
    PerspectiveState.Mutable.set(state, startState);
  }

  @Override
  public void update(double now, @NonNull PerspectiveState state) {
    Objects.requireNonNull(state);
    long deltaMs = (long) now - startTime;
    deltaMs = Math.max(deltaMs, MIN_DELTA_MS);

    float x = (float) deltaMs / (float) duration;
    x = PerspectiveUtils.clamp(x, 0, 1);
    x = blender.blend(x);

    PerspectiveState.Mutable.set(startState, currentState);
    PerspectiveState.Mutable.interpolate(x, startState, state, currentState);
  }

  @Override
  public @NonNull PerspectiveState getCurrentState() {
    return currentState;
  }
}
