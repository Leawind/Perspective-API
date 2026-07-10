package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;

public class ExpSmooth<T extends ExpSmooth.Value<T>> {
  public interface Value<T extends Value<T>> {
    T set(@NonNull T other);

    T lerp(T target, float t, T dest);
  }

  private double halflife;
  private final @NonNull T currentValue;
  private final @NonNull T targetValue;

  public ExpSmooth(Supplier<T> factory) {
    currentValue = factory.get();
    targetValue = factory.get();
  }

  public double getHalflife() {
    return halflife;
  }

  public ExpSmooth<T> setHalflife(double halflife) {
    this.halflife = halflife;
    return this;
  }

  public T target() {
    return targetValue;
  }

  public ExpSmooth<T> update(double deltaTime) {
    if (halflife <= 0) {
      currentValue.set(targetValue);
      return this;
    }
    double ratio = Math.pow(0.5, deltaTime / halflife);
    currentValue.lerp(targetValue, 1 - (float) ratio, currentValue);
    return this;
  }

  public @NonNull T current() {
    return currentValue;
  }

  @Deprecated
  public @NonNull ExpSmooth<T> setCurrentValue(T value) {
    currentValue.set(value);
    return this;
  }
}
