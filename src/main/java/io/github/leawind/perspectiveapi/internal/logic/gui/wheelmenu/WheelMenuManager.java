package io.github.leawind.perspectiveapi.internal.logic.gui.wheelmenu;

import io.github.leawind.perspectiveapi.internal.impl.PerspectiveWheelImpl;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import io.github.leawind.perspectiveapi.internal.logic.builtin.VanillaPerspective;
import io.github.leawind.perspectiveapi.internal.utils.smooth.ExpSmoothDouble;
import io.github.leawind.perspectiveapi.platform.api.Services;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector2fc;
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
  private static final WheelMenuManager INSTANCE =
      new WheelMenuManager(PerspectiveManager.INSTANCE.wheel());

  private final PerspectiveWheelImpl wheel;

  private WheelMenuManager(PerspectiveWheelImpl wheel) {
    this.wheel = wheel;
  }

  private static final int MAX_ITEMS = 9;

  /// Enable debug visuals for the wheel menu (anchor position, etc.).
  public static final boolean DEBUG = false;
  //  public static final boolean DEBUG = Services.PLATFORM_HELPER.isDevelopmentEnvironment();

  /// Invert mouse wheel direction for item rotation.
  public static final boolean INVERT_ROTATE_DIRECTION = false;

  /// Duration of the fade-in animation when the menu opens, in seconds.
  private static final double FADE_IN_DURATION_SEC = 0.2;

  /// Maximum displacement of the anchor from the origin, in GUI-scaled pixels.
  /// This is intentionally larger than the wheel radius so the player has full
  /// angular control even at small displacements.
  private static final float ANCHOR_MAX_RADIUS = 24.0f;

  public static final double ROTATE_OFFSET_RAD = -Math.PI / 2;
  private final ExpSmoothDouble smoothRotation = new ExpSmoothDouble().setHalflife(0.025);

  ExpSmoothDouble smoothRotation() {
    return smoothRotation;
  }

  double getRotateOffsetRad() {
    return ROTATE_OFFSET_RAD + smoothRotation.getCurrent();
  }

  /// Updated by:
  ///
  /// - {@link #open()}
  /// - {@link #close()}
  private boolean isOpened;

  /// Timestamp (seconds) when the menu was last opened, used for the fade-in animation.
  private double openTimeSec;

  private final Map<Identifier, WheelMenuItem> items = new HashMap<>();

  /// Updated by:
  ///
  /// - Mouse scroll event
  /// - Mouse move event
  private @Nullable WheelMenuItem selectedItem = null;

  /// Anchor position relative to the wheel center, in GUI-scaled pixels.
  ///
  /// - Set to 0,0 when open the wheel menu
  /// - Updated by mouse move event
  private final Vector2f anchor = new Vector2f(0, 0);

  /// Last known mouse position (GUI-scaled) for computing relative deltas.
  private final Vector2d lastMouse = new Vector2d(0, 0);
  private boolean hasLastMouse;

  public static @NonNull WheelMenuManager getInstance() {
    return INSTANCE;
  }

  public boolean isOpened() {
    return isOpened;
  }

  public @Nullable WheelMenuItem getSelectedItem() {
    return selectedItem;
  }

  public @NonNull WheelMenuItem getItem(Identifier id) {
    return items.computeIfAbsent(id, WheelMenuItem::new);
  }

  public @NonNull List<WheelMenuItem> getItemList() {
    // TODO cache, update in client tick
    return wheel.getSelected().stream().map(this::getItem).limit(MAX_ITEMS).toList();
  }

  /// Returns the current anchor position (for rendering the anchor indicator).
  public Vector2fc getAnchor() {
    return anchor;
  }

  /// Returns the current alpha multiplier (0.0 to 1.0) for the fade-in animation.
  /// Uses Blender's easeInOut curve (cubic smoothstep).
  @Deprecated
  public float getAlphaMultiplier() {
    double t = (GLFW.glfwGetTime() - openTimeSec) / FADE_IN_DURATION_SEC;
    if (t >= 1.0) return 1.0f;
    if (t < 0.5) return (float) (4.0 * t * t * t);
    double u = -2.0 * t + 2.0;
    return (float) (1.0 - u * u * u / 2.0);
  }

  // region lifecycle

  public void open() {
    if (Services.PLATFORM_HELPER.isDevelopmentEnvironment()) {
      // TODO
      if (wheel.getSelected().isEmpty()) {
        wheel.selectAt(0, VanillaPerspective.THIRD_PERSON_BACK.id());
        wheel.selectAt(0, VanillaPerspective.THIRD_PERSON_FRONT.id());
        wheel.selectAt(0, VanillaPerspective.FIRST_PERSON.id());
      }
    }

    isOpened = true;
    openTimeSec = GLFW.glfwGetTime();
    anchor.set(0);
    hasLastMouse = false;

    selectedItem = getItem(wheel.get());

    updatePreview();

    LOGGER.debug("Wheel menu opened with {} items", items.size());
  }

  public void close() {
    isOpened = false;
    wheel.clearPreview();
  }

  public void closeWithSelection() {
    if (!isOpened) return;

    WheelMenuItem item = selectedItem;
    close();

    if (item != null) {
      wheel.setActive(item.id());
      LOGGER.debug("Wheel menu: selected {}", item.id());
    }
  }

  public void clientTick() {
    // TODO cache item list
    getItemList().forEach(item -> item.update(PerspectiveManager.INSTANCE.registry()));
  }

  private void updatePreview() {
    WheelMenuItem item = selectedItem;
    if (item != null) {
      wheel.setPreview(item.id());
    }
  }

  // endregion

  // region input handling

  public void onMouseButton(int button, int action) {
    if (!isOpened) return;

    if (action == GLFW.GLFW_RELEASE) {
      if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
        closeWithSelection();
      } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
        close();
      }
    }
  }

  private static final boolean FIXED_SLOT_POSITION = false;

  public void onMouseScroll(double vertical) {
    if (!isOpened) return;
    boolean rotateDirection = vertical < 0 ^ INVERT_ROTATE_DIRECTION;

    var list = wheel.getSelected();

    synchronized (list) {
      // get selected item index

      int index = -1;
      var item = selectedItem;
      if (item != null) {
        index = list.indexOf(item.id());
      }

      // rotate list
      if (rotateDirection) {
        list.add(0, list.remove(list.size() - 1));
      } else {
        list.add(list.remove(0));
      }

      // reset by index
      if (FIXED_SLOT_POSITION && index >= 0) {
        selectedItem = getItem(list.get(index));
      }

      if (!FIXED_SLOT_POSITION) {
        anchor.set(0, 0);
      }
    }

    // Notify renderer for smooth rotation animation
    double sectorRad = 2 * Math.PI / list.size();
    WheelMenuRenderer.getInstance().notifyScroll(sectorRad, rotateDirection);

    updatePreview();
  }

  /// Updates the anchor based on relative mouse movement and derives the
  /// selected index from the anchor's direction.
  public void onMouseMove(double mouseX, double mouseY) {
    if (!isOpened) return;
    if (!hasLastMouse) {
      lastMouse.set(mouseX, mouseY);
      hasLastMouse = true;
      return;
    }

    double dx = mouseX - lastMouse.x();
    double dy = mouseY - lastMouse.y();
    lastMouse.set(mouseX, mouseY);

    anchor.add((float) dx, (float) dy);
    float dist = anchor.length();
    if (dist >= ANCHOR_MAX_RADIUS) {
      anchor.mul(ANCHOR_MAX_RADIUS / dist);
      var itemList = getItemList();

      if (!itemList.isEmpty()) {
        int id =
            WheelMenuUtils.getSectorIndex(
                itemList.size(), ROTATE_OFFSET_RAD, Math.atan2(anchor.x(), -anchor.y()));

        if (id < itemList.size()) {
          selectedItem = itemList.get(id);
        } else {
          // TODO
          throw new AssertionError();
        }

        updatePreview();
      }
    }
  }

  // endregion

  // region rendering data

  public record WheelMenuLayout(Vector2f center, int minEdge) {

    public WheelMenuLayout(int width, int height) {
      this(new Vector2f((float) width / 2f, (float) height / 2f), Math.min(width, height));
    }

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
