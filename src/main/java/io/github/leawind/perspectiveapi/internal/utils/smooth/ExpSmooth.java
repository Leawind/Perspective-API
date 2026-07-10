package io.github.leawind.perspectiveapi.internal.utils.smooth;

import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;

public class ExpSmooth<T extends ExpSmooth.Value<T>> {
  public interface Value<T extends Value<T>> {
    T set(@NonNull T other);

    T lerp(T target, float t, T dest);
  }

  private double halflife;
  private @NonNull T current;
  private @NonNull T target;

  public ExpSmooth(Supplier<T> factory) {
    current = factory.get();
    target = factory.get();
  }

  public double getHalflife() {
    return halflife;
  }

  public ExpSmooth<T> setHalflife(double halflife) {
    this.halflife = halflife;
    return this;
  }

  public T target() {
    return target;
  }

  public ExpSmooth<T> setTarget(T value) {
    this.target = value;
    return this;
  }

  public ExpSmooth<T> update(double deltaTime) {
    if (halflife <= 0) {
      current.set(target);
      return this;
    }
    double ratio = Math.pow(0.5, deltaTime / halflife);
    current.lerp(target, 1 - (float) ratio, current);
    return this;
  }

  public @NonNull T current() {
    return current;
  }

  public @NonNull ExpSmooth<T> setCurrent(T value) {
    current = value;
    return this;
  }
}
