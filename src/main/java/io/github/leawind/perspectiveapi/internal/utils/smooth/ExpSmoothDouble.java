package io.github.leawind.perspectiveapi.internal.utils.smooth;

public final class ExpSmoothDouble {
  private double halflife;

  private double current;
  private double target;

  public double getHalflife() {
    return halflife;
  }

  public ExpSmoothDouble setHalflife(double halflife) {
    this.halflife = halflife;
    return this;
  }

  public ExpSmoothDouble setTarget(double target) {
    this.target = target;
    return this;
  }

  /**
   * @param deltaTime Must be greater than 0
   */
  public ExpSmoothDouble update(double deltaTime) {
    if (halflife <= 0) {
      current = target;
      return this;
    }

    double ratio = Math.pow(0.5, deltaTime / halflife);
    current += (target - current) * (1 - ratio);

    return this;
  }

  public double getCurrent() {
    return current;
  }

  public ExpSmoothDouble setCurrent(double current) {
    this.current = current;
    return this;
  }
}
