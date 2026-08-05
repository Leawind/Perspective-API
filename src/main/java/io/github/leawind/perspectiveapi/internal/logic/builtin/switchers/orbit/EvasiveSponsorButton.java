package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import java.util.Locale;
import org.joml.Vector2d;

final class EvasiveSponsorButton {
  static final int HOME_INSET = 18;

  private static final double REPULSION_RANGE = 112;
  private static final double REPULSION_FACTOR = 8_000_000;
  private static final double MIN_REPULSION_DISTANCE = 8;
  private static final double MAX_REPULSION_FORCE = 16_000;
  private static final double HOME_SPRING = 20;
  private static final double LINEAR_DRAG = 8;
  private static final double MAX_SPEED = 850;
  private static final double MAX_FRAME_SECONDS = 0.05;
  private static final double FORCED_ESCAPE_GAP = 10;

  private final Vector2d position = new Vector2d();
  private final Vector2d velocity = new Vector2d();
  private boolean initialized;
  private boolean visible;
  private int width;
  private int height;
  private int screenWidth;
  private int screenHeight;

  void reset() {
    initialized = false;
    visible = false;
    velocity.zero();
  }

  void update(
      int screenWidth,
      int screenHeight,
      int width,
      int height,
      double mouseX,
      double mouseY,
      double frameSeconds) {
    this.screenWidth = screenWidth;
    this.screenHeight = screenHeight;
    this.width = width;
    this.height = height;
    visible = fitsOnScreen();
    if (!visible) return;

    if (!initialized) {
      initialized = true;
      position.set(homeX(), homeY());
      velocity.zero();
    } else {
      constrainToScreen();
    }

    double deltaSeconds = Math.min(Math.max(frameSeconds, 0), MAX_FRAME_SECONDS);
    if (deltaSeconds > 0) integrate(mouseX, mouseY, deltaSeconds);
    constrainToScreen();

    if (contains(mouseX, mouseY) && !forceEscape(mouseX, mouseY)) visible = false;
  }

  boolean isVisible() {
    return visible;
  }

  int x() {
    return (int) Math.round(position.x);
  }

  int y() {
    return (int) Math.round(position.y);
  }

  int width() {
    return width;
  }

  int height() {
    return height;
  }

  boolean contains(double x, double y) {
    if (!visible) return false;
    int left = x();
    int top = y();
    return x >= left && x < left + width && y >= top && y < top + height;
  }

  static boolean isClickEnabled(String languageCode) {
    String normalized = languageCode.toLowerCase(Locale.ROOT).replace('-', '_');
    return normalized.equals("zh") || normalized.startsWith("zh_") || normalized.equals("lzh");
  }

  static double repulsionMagnitude(double distance) {
    double effectiveDistance = Math.max(distance, MIN_REPULSION_DISTANCE);
    return Math.min(
        REPULSION_FACTOR / (effectiveDistance * effectiveDistance), MAX_REPULSION_FORCE);
  }

  private void integrate(double mouseX, double mouseY, double deltaSeconds) {
    double left = position.x;
    double top = position.y;
    double right = left + width;
    double bottom = top + height;
    double nearestX = clamp(mouseX, left, right);
    double nearestY = clamp(mouseY, top, bottom);
    double distance = Math.hypot(nearestX - mouseX, nearestY - mouseY);
    double awayX = centerX() - mouseX;
    double awayY = centerY() - mouseY;
    double directionLength = Math.hypot(awayX, awayY);

    if (directionLength < 1e-9) {
      awayX = position.x <= (minX() + maxX()) * 0.5 ? 1 : -1;
      directionLength = 1;
    }

    double forceX = (homeX() - position.x) * HOME_SPRING - velocity.x * LINEAR_DRAG;
    double forceY = (homeY() - position.y) * HOME_SPRING - velocity.y * LINEAR_DRAG;
    if (distance < REPULSION_RANGE) {
      double magnitude = repulsionMagnitude(distance);
      forceX += awayX / directionLength * magnitude;
      forceY += awayY / directionLength * magnitude;
    }

    velocity.add(forceX * deltaSeconds, forceY * deltaSeconds);
    double speed = velocity.length();
    if (speed > MAX_SPEED) velocity.mul(MAX_SPEED / speed);
    position.fma(deltaSeconds, velocity);
  }

  private boolean forceEscape(double mouseX, double mouseY) {
    double currentX = position.x;
    double currentY = position.y;
    double bestX = currentX;
    double bestY = currentY;
    double bestDistanceSquared = Double.POSITIVE_INFINITY;

    double[][] candidates = {
      {mouseX - width - FORCED_ESCAPE_GAP, currentY},
      {mouseX + FORCED_ESCAPE_GAP, currentY},
      {currentX, mouseY - height - FORCED_ESCAPE_GAP},
      {currentX, mouseY + FORCED_ESCAPE_GAP},
      {minX(), minY()},
      {maxX(), minY()},
      {minX(), maxY()},
      {maxX(), maxY()}
    };

    for (double[] candidate : candidates) {
      double candidateX = clamp(candidate[0], minX(), maxX());
      double candidateY = clamp(candidate[1], minY(), maxY());
      if (containsAt(candidateX, candidateY, mouseX, mouseY)) continue;
      double dx = candidateX - currentX;
      double dy = candidateY - currentY;
      double distanceSquared = dx * dx + dy * dy;
      if (distanceSquared < bestDistanceSquared) {
        bestDistanceSquared = distanceSquared;
        bestX = candidateX;
        bestY = candidateY;
      }
    }

    if (!Double.isFinite(bestDistanceSquared)) return false;
    position.set(bestX, bestY);
    double escapeX = bestX - currentX;
    double escapeY = bestY - currentY;
    double escapeDistance = Math.hypot(escapeX, escapeY);
    if (escapeDistance > 1e-9) {
      velocity.set(escapeX / escapeDistance, escapeY / escapeDistance).mul(MAX_SPEED * 0.45);
    } else {
      velocity.zero();
    }
    return true;
  }

  private void constrainToScreen() {
    double constrainedX = clamp(position.x, minX(), maxX());
    double constrainedY = clamp(position.y, minY(), maxY());
    if (constrainedX != position.x
        && Math.signum(velocity.x) == Math.signum(position.x - constrainedX)) {
      velocity.x = 0;
    }
    if (constrainedY != position.y
        && Math.signum(velocity.y) == Math.signum(position.y - constrainedY)) {
      velocity.y = 0;
    }
    position.set(constrainedX, constrainedY);
  }

  private boolean fitsOnScreen() {
    return width > 0 && height > 0 && screenWidth >= width && screenHeight >= height;
  }

  private boolean containsAt(double left, double top, double x, double y) {
    return x >= left && x < left + width && y >= top && y < top + height;
  }

  private double centerX() {
    return position.x + width * 0.5;
  }

  private double centerY() {
    return position.y + height * 0.5;
  }

  private double minX() {
    return 0;
  }

  private double maxX() {
    return screenWidth - width;
  }

  private double minY() {
    return 0;
  }

  private double maxY() {
    return screenHeight - height;
  }

  private double homeX() {
    return Math.min(HOME_INSET, maxX());
  }

  private double homeY() {
    return Math.max(maxY() - HOME_INSET, minY());
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
