package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EvasiveSponsorButtonTest {
  @Test
  void startsInsetFromBottomLeftCorner() {
    EvasiveSponsorButton button = new EvasiveSponsorButton();

    button.update(320, 180, 60, 160, 20, 0);

    assertTrue(button.isVisible());
    assertEquals(EvasiveSponsorButton.HOME_INSET, button.x());
    assertEquals(180 - EvasiveSponsorButton.HOME_INSET - EvasiveSponsorButton.HEIGHT, button.y());
  }

  @Test
  void inverseSquareRepulsionGetsFourTimesStrongerAtHalfDistance() {
    assertEquals(
        EvasiveSponsorButton.repulsionMagnitude(80) * 4,
        EvasiveSponsorButton.repulsionMagnitude(40),
        1e-9);
  }

  @Test
  void cursorBelowLeftSidePushesButtonUpAndRight() {
    EvasiveSponsorButton button = new EvasiveSponsorButton();
    button.update(320, 180, 43, 160, 20, 0);
    int initialX = button.x();
    int initialY = button.y();

    button.update(320, 180, 43, 5, 179, 1.0 / 60.0);

    assertTrue(button.x() > initialX);
    assertTrue(button.y() < initialY);
  }

  @Test
  void forcedEscapeSlidesAlongScreenEdge() {
    EvasiveSponsorButton button = new EvasiveSponsorButton();
    button.update(320, 180, 60, 160, 20, 0);
    double mouseX = button.x() + button.width() * 0.5;
    double mouseY = button.y() + EvasiveSponsorButton.HEIGHT * 0.5;

    button.update(320, 180, 60, mouseX, mouseY, 0);

    assertFalse(button.contains(mouseX, mouseY));
    assertEquals(EvasiveSponsorButton.HOME_INSET, button.x());
    assertTrue(button.y() < 180 - EvasiveSponsorButton.HOME_INSET - EvasiveSponsorButton.HEIGHT);
  }

  @Test
  void forcedEscapeMayTouchScreenEdge() {
    EvasiveSponsorButton button = new EvasiveSponsorButton();
    button.update(320, 180, 43, 160, 20, 0);
    double mouseX = 43;
    double mouseY = button.y() + EvasiveSponsorButton.HEIGHT * 0.5;

    button.update(320, 180, 43, mouseX, mouseY, 0);

    assertEquals(0, button.x());
    assertFalse(button.contains(mouseX, mouseY));
  }

  @Test
  void remainsOnScreenWhenCursorRepeatedlyCatchesIt() {
    EvasiveSponsorButton button = new EvasiveSponsorButton();
    int screenWidth = 320;
    int screenHeight = 180;
    int buttonWidth = 60;
    button.update(screenWidth, screenHeight, buttonWidth, 160, 20, 0);

    for (int frame = 0; frame < 500; frame++) {
      double mouseX = button.x() + button.width() * 0.5;
      double mouseY = button.y() + EvasiveSponsorButton.HEIGHT * 0.5;
      button.update(screenWidth, screenHeight, buttonWidth, mouseX, mouseY, 1.0 / 60.0);

      assertTrue(button.isVisible());
      assertFalse(button.contains(mouseX, mouseY));
      assertTrue(button.x() >= 0);
      assertTrue(button.x() + button.width() <= screenWidth);
      assertTrue(button.y() >= 0);
      assertTrue(button.y() + EvasiveSponsorButton.HEIGHT <= screenHeight);
    }
  }

  @Test
  void hidesWhenScreenCannotContainIt() {
    EvasiveSponsorButton button = new EvasiveSponsorButton();

    button.update(59, 15, 60, 0, 0, 0);

    assertFalse(button.isVisible());
  }

  @Test
  void clickingIsEnabledForEveryChineseLanguageVariantOnly() {
    assertTrue(EvasiveSponsorButton.isClickEnabled("zh_cn"));
    assertTrue(EvasiveSponsorButton.isClickEnabled("zh_tw"));
    assertTrue(EvasiveSponsorButton.isClickEnabled("zh-hk"));
    assertTrue(EvasiveSponsorButton.isClickEnabled("lzh"));
    assertFalse(EvasiveSponsorButton.isClickEnabled("en_us"));
    assertFalse(EvasiveSponsorButton.isClickEnabled("ja_jp"));
  }
}
