package io.github.leawind.perspectiveapi.internal.utils.smooth;

import io.github.leawind.perspectiveapi.internal.utils.Utils;
import org.jspecify.annotations.NonNull;

public final class FixedTimeSmoothDouble {
  private double duration;

  private double startTime;

  private double start;
  private double current;
  private double target;

  private @NonNull Blender blender = Blenders::easeInOut;

  public FixedTimeSmoothDouble setBlender(Blender blender) {
    this.blender = blender;
    return this;
  }

  public FixedTimeSmoothDouble setDuration(double duration) {
    this.duration = duration;
    return this;
  }

  public double getDuration() {
    return duration;
  }

  public FixedTimeSmoothDouble setStart(double now, double value) {
    startTime = now;
    start = value;
    return this;
  }

  public FixedTimeSmoothDouble setTarget(double target) {
    this.target = target;
    return this;
  }

  public FixedTimeSmoothDouble update(double now) {
    float ratio = (float) ((now - startTime) / duration);
    double eased = blender.blend(ratio);
    eased = Utils.clamp(eased, 0, 1);
    current = start + (target - start) * eased;
    return this;
  }

  public double getCurrent() {
    return current;
  }

  public FixedTimeSmoothDouble setCurrent(double value) {
    current = value;
    return this;
  }
}
