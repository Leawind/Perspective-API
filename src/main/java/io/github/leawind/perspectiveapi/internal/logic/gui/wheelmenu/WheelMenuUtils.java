package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

public final class WheelMenuUtils {
  private WheelMenuUtils() {}

  public static int getSectorIndex(int sectorCount, double rotateOffsetRad, double angleRad) {
    if (sectorCount <= 0) return -1;

    double sectorRad = 2 * Math.PI / sectorCount;
    double angleRadWithOffset = angleRad - rotateOffsetRad - sectorRad / 2;

    double normalized =
        angleRadWithOffset - Math.floor(angleRadWithOffset / (2 * Math.PI)) * (2 * Math.PI);
    if (normalized < 0 || normalized >= 2 * Math.PI) {
      return -1;
    }
    int index = (int) (normalized / sectorRad);
    if (index >= sectorCount) {
      return sectorCount - 1;
    }

    return index;
  }
}
