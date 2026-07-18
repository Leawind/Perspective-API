package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.wheel;

public final class WheelMenuUtils {
  private WheelMenuUtils() {}

  public static int getSectorIndex(
      int sectorCount, double rotateOffsetRad, double angleRadWithOffset) {
    if (sectorCount <= 0) return -1;

    final double halfSectorRad = Math.PI / sectorCount;
    final double sectorRad = halfSectorRad * 2;

    double angleRad = angleRadWithOffset - rotateOffsetRad + halfSectorRad;

    final double normalized = angleRad - Math.floor(angleRad / (2 * Math.PI)) * (2 * Math.PI);

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
