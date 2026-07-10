package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveWheelImpl;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.joml.Vector2d;
import org.joml.Vector2dc;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the lifecycle and state of the perspective wheel menu overlay.
///
/// Selection uses an anchor point model: the anchor starts at the origin and
/// moves with the cursor (as a relative displacement). It is clamped to a
/// circle of {@link #ANCHOR_MAX_RADIUS} so the direction from the origin to the
/// anchor always stays well-defined. The direction determines which sector
/// (and therefore which perspective) is currently selected.
public final class WheelMenuManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(WheelMenuManager.class);
  private static final WheelMenuManager INSTANCE = new WheelMenuManager();

  private static final int MAX_ITEMS = 9;

  /// Enable debug visuals for the wheel menu (anchor position, etc.).
  public static final boolean DEBUG = false;
//  public static final boolean DEBUG = Services.PLATFORM_HELPER.isDevelopmentEnvironment();

  /// Duration of the fade-in animation when the menu opens, in seconds.
  private static final double FADE_IN_DURATION_SEC = 0.2;

  /// Maximum displacement of the anchor from the origin, in GUI-scaled pixels.
  /// This is intentionally larger than the wheel radius so the player has full
  /// angular control even at small displacements.
  private static final float ANCHOR_MAX_RADIUS = 24.0f;

  public static final double ROTATE_OFFSET_RAD = -Math.PI / 2;
  
  private boolean isVisible;

  /// Timestamp (seconds) when the menu was last opened, used for the fade-in animation.
  private double openTimeSec;

  /// -1 means unselecetd
  private int selectedIndex = -1;

  private List<WheelMenuItem> items = Collections.emptyList();

  /// Anchor position relative to the wheel center, in GUI-scaled pixels.
  private final Vector2d anchor = new Vector2d(0, 0);

  /// Last known mouse position (GUI-scaled) for computing relative deltas.
  private final Vector2d lastMouse = new Vector2d(0, 0);
  private boolean hasLastMouse;

  private WheelMenuManager() {}

  public static @NonNull WheelMenuManager getInstance() {
    return INSTANCE;
  }

  public boolean isVisible() {
    return isVisible;
  }

  public int getSelectedIndex() {
    return selectedIndex;
  }

  public @Nullable WheelMenuItem getSelectedItem() {
    if (!isVisible || items.isEmpty()) return null;
    int idx = Math.floorMod(selectedIndex, items.size());
    return items.get(idx);
  }

  public @NonNull List<WheelMenuItem> getItems() {
    return items;
  }

  /// Returns the current anchor position (for rendering the anchor indicator).
  public Vector2dc getAnchor() {
    return anchor;
  }

  /// Returns the current alpha multiplier (0.0 to 1.0) for the fade-in animation.
  /// Uses Blender's easeInOut curve (cubic smoothstep).
  public float getAlphaMultiplier() {
    double t = (GLFW.glfwGetTime() - openTimeSec) / FADE_IN_DURATION_SEC;
    if (t >= 1.0) return 1.0f;
    if (t < 0.5) return (float) (4.0 * t * t * t);
    double u = -2.0 * t + 2.0;
    return (float) (1.0 - u * u * u / 2.0);
  }

  // region lifecycle

  public void open() {
    PerspectiveRegistry registry = PerspectiveAPI.getRegistry();
    PerspectiveWheelImpl wheel = PerspectiveManager.INSTANCE.wheel();

    List<Identifier> ordered = wheel.getOrdered();

    List<WheelMenuItem> items = new ArrayList<>();
    int limit = Math.min(ordered.size(), MAX_ITEMS);
    for (int i = 0; i < limit; i++) {
      Identifier id = ordered.get(i);
      Perspective perspective = registry.get(id);
      WheelMenuItem.Availability availability;
      if (perspective == null) {
        availability = WheelMenuItem.Availability.UNREGISTERED;
      } else if (!perspective.isAvailable()) {
        availability = WheelMenuItem.Availability.UNAVAILABLE;
      } else {
        availability = WheelMenuItem.Availability.AVAILABLE;
      }
      items.add(new WheelMenuItem(id, perspective, availability));
    }

    this.items = Collections.unmodifiableList(items);

    selectedIndex = ordered.indexOf(wheel.get());
    if (selectedIndex < 0) selectedIndex = 0;

    isVisible = true;
    openTimeSec = GLFW.glfwGetTime();
    anchor.set(0);
    hasLastMouse = false;

    updatePreview();

    LOGGER.debug("Wheel menu opened with {} items", items.size());
  }

  public void close() {
    isVisible = false;
    items = Collections.emptyList();
    PerspectiveManager.INSTANCE.wheel().clearPreview();
    LOGGER.debug("Wheel menu closed");
  }

  public void closeWithSelection() {
    if (!isVisible) return;

    WheelMenuItem item = getSelectedItem();
    close();

    if (item != null) {
      PerspectiveManager.INSTANCE.wheel().setActive(item.id());
      LOGGER.debug("Wheel menu: selected {}", item.id());
    }
  }

  private void updatePreview() {
    WheelMenuItem item = getSelectedItem();
    if (item != null) {
      PerspectiveManager.INSTANCE.wheel().setPreview(item.id());
    }
  }

  // endregion

  // region input handling

  public void onMouseButton(int button, int action) {
    if (!isVisible) return;

    if (action == GLFW.GLFW_RELEASE) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
        closeWithSelection();
      } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
        close();
      }
    }
  }

  public void onMouseScroll(double vertical) {
    if (!isVisible) return;
    if (items.isEmpty()) return;

    int n = items.size();
    if (vertical > 0) {
      selectedIndex = (selectedIndex - 1 + n) % n;
    } else if (vertical < 0) {
      selectedIndex = (selectedIndex + 1) % n;
    }
    updatePreview();
  }

  /// Updates the anchor based on relative mouse movement and derives the
  /// selected index from the anchor's direction.
  public void onMouseMove(double mouseX, double mouseY) {
    if (!isVisible) return;
    if (!hasLastMouse) {
      lastMouse.set(mouseX, mouseY);
      hasLastMouse = true;
      return;
    }

    double dx = mouseX - lastMouse.x();
    double dy = mouseY - lastMouse.y();
    lastMouse.set(mouseX, mouseY);

    anchor.add(dx, dy);
    double dist = anchor.length();
    if (dist >= ANCHOR_MAX_RADIUS) {
      anchor.mul(ANCHOR_MAX_RADIUS / dist);
      if (!items.isEmpty()) {
        selectedIndex = WheelMenuUtils.getSectorIndex(
            items.size(), ROTATE_OFFSET_RAD, Math.atan2(anchor.x(), -anchor.y()));
        updatePreview();
      }
    }
  }

  // endregion

  // region rendering data

  public @NonNull WheelMenuLayout getLayout(int screenWidth, int screenHeight) {
    float minEdge = Math.min(screenWidth, screenHeight);
    return new WheelMenuLayout(screenWidth / 2.0f, screenHeight / 2.0f, minEdge);
  }

  public record WheelMenuLayout(float centerX, float centerY, float minEdge) {

    public float iconRadius() {
      return minEdge * 0.25f;
    }

    public float iconSize() {
      return minEdge * 0.08f;
    }

    public float centerIconSize() {
      return minEdge * 0.1f;
    }
  }

  // endregion
}
