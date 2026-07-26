package io.github.leawind.perspectiveapi.internal.utils.smooth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class SmoothTest {
  private static final double DELTA = 1.0e-9;

  @Test
  void blendersHaveExpectedEndpointsAndMidpoints() {
    assertEquals(0.0f, Blenders.linear(0.0f));
    assertEquals(0.5f, Blenders.linear(0.5f));
    assertEquals(1.0f, Blenders.linear(1.0f));

    assertEquals(0.0f, Blenders.easeInOut(0.0f));
    assertEquals(0.5f, Blenders.easeInOut(0.5f));
    assertEquals(1.0f, Blenders.easeInOut(1.0f));
    assertEquals(0.25f, Blenders.easeIn(0.5f));
    assertEquals(0.75f, Blenders.easeOut(0.5f));
    assertEquals(1.0f - Math.sqrt(0.5), Blenders.sineIn(0.5f), 1.0e-6);
    assertEquals(Math.sqrt(0.5), Blenders.sineOut(0.5f), 1.0e-6);
    assertEquals(0.5f, Blenders.sineInOut(0.5f), 1.0e-6f);
  }

  @Test
  void exponentialDoubleMovesHalfwayPerHalflife() {
    ExpSmoothDouble smooth = new ExpSmoothDouble().setCurrent(2.0).setTarget(10.0).setHalflife(4.0);

    assertSame(smooth, smooth.update(4.0));
    assertEquals(6.0, smooth.getCurrent(), DELTA);

    smooth.update(4.0);
    assertEquals(8.0, smooth.getCurrent(), DELTA);
    assertEquals(4.0, smooth.getHalflife(), DELTA);
  }

  @Test
  void nonPositiveHalflifeSnapsToTarget() {
    ExpSmoothDouble smooth = new ExpSmoothDouble().setCurrent(2.0).setTarget(10.0).setHalflife(0.0);

    smooth.update(0.0);

    assertEquals(10.0, smooth.getCurrent(), DELTA);
  }

  @Test
  void genericExponentialSmoothUpdatesMutableValues() {
    ExpSmooth<TestVector> smooth =
        new ExpSmooth<>(TestVector::new)
            .setCurrent(new TestVector(0.0f))
            .setTarget(new TestVector(8.0f))
            .setHalflife(2.0);

    smooth.update(2.0);

    assertEquals(4.0f, smooth.current().value, 1.0e-6f);
    assertEquals(2.0, smooth.getHalflife(), DELTA);
    assertEquals(8.0f, smooth.target().value);
  }

  @Test
  void fixedTimeSmoothUsesConfiguredBlenderAndClampsTime() {
    FixedTimeSmoothDouble smooth =
        new FixedTimeSmoothDouble()
            .setDuration(10.0)
            .setStart(5.0, 20.0)
            .setTarget(40.0)
            .setBlender(Blenders::linear);

    assertSame(smooth, smooth.update(0.0));
    assertEquals(20.0, smooth.getCurrent(), DELTA);

    smooth.update(10.0);
    assertEquals(30.0, smooth.getCurrent(), DELTA);

    smooth.update(20.0);
    assertEquals(40.0, smooth.getCurrent(), DELTA);
    assertEquals(10.0, smooth.getDuration(), DELTA);
  }

  private static final class TestVector implements ExpSmooth.Value<TestVector> {
    private float value;

    private TestVector() {}

    private TestVector(float value) {
      this.value = value;
    }

    @Override
    public TestVector set(TestVector other) {
      value = other.value;
      return this;
    }

    @Override
    public TestVector lerp(TestVector target, float t, TestVector dest) {
      dest.value = value + (target.value - value) * t;
      return dest;
    }
  }
}
