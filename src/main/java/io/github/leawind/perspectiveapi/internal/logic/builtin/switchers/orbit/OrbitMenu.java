package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import io.github.leawind.perspectiveapi.internal.bridge.events.GameClientEvents;
import io.github.leawind.perspectiveapi.internal.bridge.events.GuiRenderContext;
import io.github.leawind.perspectiveapi.internal.bridge.events.MouseInputContext;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.OrbitSwitcherModel.Group;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics.BodyType;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics.DragForce;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics.InverseSquareForces;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics.PairForce;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics.PhysicsBody;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.physics.PhysicsWorld;
import io.github.leawind.perspectiveapi.internal.utils.WheelAnchor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.joml.Vector2d;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

final class OrbitMenu {
  enum Mode {
    SELECTING,
    EDITING
  }

  static final double ICON_SIZE = 0.075;
  static final double RING_RADIUS = 0.25;
  private static final double CLICK_MAX_DISTANCE_PX = 4;
  private static final double GROUP_INNER_BOUNDARY = 0.22;
  private static final double GROUP_OUTER_BOUNDARY = 0.32;
  private static final double CANDIDATE_MARGIN = 0.08;
  private static final double CANDIDATE_SPACING = 0.1;
  private static final double LINEAR_DRAG_FACTOR = 12;
  private static final double LAYOUT_SPRING_MAX_FORCE = 160;
  private static final String SPONSOR_URL = "https://leawind.github.io/zh_cn/donate?autolang";
  private static final PairForce DISABLED_REPULSION = InverseSquareForces.coulomb(0.025, 0.035, 10);

  private final OrbitSwitcherBehavior owner;
  private final OrbitSwitcherModel model;
  private final PhysicsWorld world = new PhysicsWorld();
  private final Map<String, PerspectiveActor> actors = new HashMap<>();
  private final OrbitMenuRenderer renderer = new OrbitMenuRenderer();
  private final EvasiveSponsorButton sponsorButton = new EvasiveSponsorButton();

  private boolean initialized;
  private boolean restoreMouseGrab;
  private @Nullable PerspectiveActor grabbedActor;
  private @Nullable PerspectiveActor hoveredActor;
  private boolean opened;
  private Mode mode = Mode.SELECTING;

  private int screenWidth = 1;
  private int screenHeight = 1;
  private double minEdge = 1;
  private final Vector2d mouseScreen = new Vector2d();
  private final Vector2d mouseWorld = new Vector2d();
  private final Vector2d lastMouseScreen = new Vector2d();
  private final Vector2d grabStartMouseScreen = new Vector2d();
  private final Vector2d candidateTarget = new Vector2d();
  private final WheelAnchor wheelAnchor = new WheelAnchor(24);
  private boolean hasLastMouse;
  private long lastRenderNanos;

  OrbitMenu(@NonNull OrbitSwitcherBehavior owner, @NonNull OrbitSwitcherModel model) {
    this.owner = Objects.requireNonNull(owner);
    this.model = Objects.requireNonNull(model);
    world.setMaxTotalForce(320);

    world.setMaxSpeed(15);
    world.addPairForce(InverseSquareForces.coulomb(0.0005, 0.025, 1));
    world.addPairForce(this::applyDisabledRepulsion);
    world.addBodyForce(this::applyLayoutForces);
    world.addBodyForce(new DragForce(LINEAR_DRAG_FACTOR, 1.5));
    world.addBodyForce(OrbitMenu::applyFriction);
  }

  void init() {
    if (initialized) return;
    initialized = true;
    GameClientEvents.MOUSE_INPUT.on(this::onMouseInput);
    GameClientEvents.RENDER_GUI_OVERLAY.on(this::render);
  }

  void syncActors() {
    Set<String> desired = new HashSet<>();
    for (Perspective perspective : model.switchables()) {
      String id = perspective.info().id();
      desired.add(id);
      if (!actors.containsKey(id)) {
        PerspectiveActor actor = new PerspectiveActor(id);
        actors.put(id, actor);
        world.addBody(actor.body());
        placeInitially(actor, model.groupOf(id));
      }
    }

    List<String> removed = actors.keySet().stream().filter(id -> !desired.contains(id)).toList();
    for (String id : removed) {
      PerspectiveActor actor = actors.remove(id);
      if (actor != null) world.removeBody(actor.body());
    }
  }

  void open() {
    if (!initialized || opened || hasOpenScreen() || !isOwnerActive()) return;
    opened = true;
    mode = Mode.SELECTING;
    grabbedActor = null;
    hoveredActor = null;
    hasLastMouse = false;
    resetWheelAnchor();
    lastRenderNanos = 0;
    world.resetClock();
    owner.onMenuOpened();
  }

  void closeFromKey() {
    if (!opened) return;
    close();
  }

  void close() {
    if (!opened) return;
    releaseActor(false);
    closeCursorLease();
    model.clearPreview();
    opened = false;
    hoveredActor = null;
    wheelAnchor.reset();
    owner.onMenuClosed();
  }

  boolean isOpened() {
    return opened;
  }

  @NonNull Mode mode() {
    return mode;
  }

  @NonNull Collection<@NonNull PerspectiveActor> actors() {
    return List.copyOf(actors.values());
  }

  @Nullable PerspectiveActor actor(@NonNull String id) {
    return actors.get(Objects.requireNonNull(id));
  }

  @Nullable PerspectiveActor hoveredActor() {
    return hoveredActor;
  }

  @Nullable PerspectiveActor grabbedActor() {
    return grabbedActor;
  }

  OrbitSwitcherModel model() {
    return model;
  }

  EvasiveSponsorButton sponsorButton() {
    return sponsorButton;
  }

  double worldToScreenX(double x) {
    return screenWidth * 0.5 + x * minEdge;
  }

  double worldToScreenY(double y) {
    return screenHeight * 0.5 + y * minEdge;
  }

  int mouseScreenX() {
    return (int) mouseScreen.x;
  }

  int mouseScreenY() {
    return (int) mouseScreen.y;
  }

  boolean isMouseOver(int x, int y, int width, int height) {
    return mouseScreen.x >= x
        && mouseScreen.x < x + width
        && mouseScreen.y >= y
        && mouseScreen.y < y + height;
  }

  private void onMouseInput(@NonNull MouseInputContext context) {
    if (!opened) return;
    if (!isOwnerActive()) {
      close();
      return;
    }

    switch (context.type) {
      case BUTTON -> onMouseButton(context.button, context.action, context.mouseX, context.mouseY);
      case SCROLL -> onMouseScroll(context.scrollDelta);
      case MOVE -> onMouseMove(context.mouseX, context.mouseY);
    }
    context.consumed = true;
  }

  private void onMouseButton(int button, int action, double mouseX, double mouseY) {
    if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) return;
    mouseScreen.set(mouseX, mouseY);
    updateMouseWorld();

    if (mode == Mode.SELECTING) {
      if (action == GLFW.GLFW_PRESS && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
        enterEditing();
      }
      return;
    }

    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
      if (action == GLFW.GLFW_PRESS) {
        if (sponsorButton.contains(mouseX, mouseY)) {
          String languageCode = Minecraft.getInstance().getLanguageManager().getSelected();
          if (EvasiveSponsorButton.isClickEnabled(languageCode)) Bridge.openUri(SPONSOR_URL);
          return;
        }
        if (renderer.isEditingHelpButtonHovered(this)) return;
        grabHoveredActor();
      } else {
        releaseActor(true);
      }
    } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS) {
      exitEditing();
    }
  }

  private void onMouseScroll(double delta) {
    if (mode != Mode.SELECTING || delta == 0) return;
    model.rotateSelected(delta < 0);
    wheelAnchor.reset();
  }

  private void onMouseMove(double x, double y) {
    mouseScreen.set(x, y);
    updateMouseWorld();
    if (!hasLastMouse) {
      lastMouseScreen.set(mouseScreen);
      hasLastMouse = true;
      updateEditingHover();
      return;
    }

    if (mode == Mode.SELECTING) {
      wheelAnchor.moveBy((float) (x - lastMouseScreen.x), (float) (y - lastMouseScreen.y));
      updateSelectingHover();
    } else {
      if (grabbedActor != null) {
        grabbedActor.dragTo(mouseWorld);
        grabbedActor.body().velocity().zero();
        updateDraggedLayout(grabbedActor);
      }
      updateEditingHover();
    }
    lastMouseScreen.set(mouseScreen);
  }

  private void enterEditing() {
    mode = Mode.EDITING;
    owner.onEditingOpened();
    model.clearPreview();
    Minecraft minecraft = Minecraft.getInstance();
    restoreMouseGrab = minecraft.mouseHandler.isMouseGrabbed();
    if (restoreMouseGrab) minecraft.mouseHandler.releaseMouse();
    hasLastMouse = false;
    hoveredActor = null;
    sponsorButton.reset();
  }

  private void exitEditing() {
    releaseActor(false);
    closeCursorLease();
    model.clearPreview();
    mode = Mode.SELECTING;
    hasLastMouse = false;
    hoveredActor = null;
    resetWheelAnchor();
  }

  private void grabHoveredActor() {
    if (hoveredActor == null) return;
    grabbedActor = hoveredActor;
    grabStartMouseScreen.set(mouseScreen);
    grabbedActor.beginDrag(mouseWorld);
    grabbedActor.body().setType(BodyType.KINEMATIC);
    grabbedActor.body().velocity().zero();
  }

  private void releaseActor(boolean allowClick) {
    if (grabbedActor == null) return;
    PerspectiveActor releasedActor = grabbedActor;
    updateDraggedLayout(releasedActor);
    releasedActor.body().velocity().zero();
    releasedActor.body().setType(BodyType.DYNAMIC);
    grabbedActor = null;
    if (allowClick
        && isShortDrag(
            mouseScreen.x - grabStartMouseScreen.x, mouseScreen.y - grabStartMouseScreen.y)) {
      model.activate(releasedActor.perspectiveId());
    }
  }

  static boolean isShortDrag(double deltaX, double deltaY) {
    return deltaX * deltaX + deltaY * deltaY <= CLICK_MAX_DISTANCE_PX * CLICK_MAX_DISTANCE_PX;
  }

  private void closeCursorLease() {
    if (!restoreMouseGrab) return;
    restoreMouseGrab = false;
    Minecraft minecraft = Minecraft.getInstance();
    if (Bridge.getScreen(minecraft) == null) minecraft.mouseHandler.grabMouse();
  }

  private void updateSelectingHover() {
    if (!wheelAnchor.isOnBorder()) return;

    List<String> selected = model.selected();
    if (selected.isEmpty()) {
      setHovered(null);
      return;
    }
    int index = nearestWheelSlot(wheelAnchor.getAngleRad() + Math.PI / 2, selected.size());
    setHovered(actors.get(selected.get(index)));
  }

  private void resetWheelAnchor() {
    wheelAnchor.reset().setRadius(Math.max(8 * model.selected().size(), 24));
  }

  private void updateEditingHover() {
    if (mode != Mode.EDITING || grabbedActor != null) return;
    PerspectiveActor best = null;
    double bestDistance = ICON_SIZE * 0.65;
    for (PerspectiveActor actor : actors.values()) {
      double distance = actor.body().position().distance(mouseWorld);
      if (distance <= bestDistance) {
        best = actor;
        bestDistance = distance;
      }
    }
    setHovered(best);
  }

  private void setHovered(@Nullable PerspectiveActor actor) {
    hoveredActor = actor;
    if (mode == Mode.SELECTING) {
      if (actor != null && isAvailable(actor)) model.activate(actor.perspectiveId());
      return;
    }
    if (actor != null && isAvailable(actor)) model.preview(actor.perspectiveId());
    else model.clearPreview();
  }

  /// Treats only the dragged actor's position as player input. Positions produced by the physics
  /// simulation must not reorder the model, or moving targets can create a self-sustaining orbit.
  private void updateDraggedLayout(PerspectiveActor actor) {
    if (mode != Mode.EDITING) return;
    String id = actor.perspectiveId();
    Group newGroup = classify(model.groupOf(id), actor.body().position().x);
    List<String> selected = new ArrayList<>(model.selected());
    selected.remove(id);
    Set<String> disabled = new HashSet<>(model.disabled());
    disabled.remove(id);

    if (newGroup == Group.SELECTED) {
      selected = moveToNearestWheelSlot(selected, id, wheelAngle(actor));
    } else if (newGroup == Group.DISABLED) {
      disabled.add(id);
    }
    model.applyLayout(selected, disabled);
  }

  static List<String> moveToNearestWheelSlot(
      List<String> currentOrder, String draggedId, double angleRad) {
    List<String> newOrder = new ArrayList<>(currentOrder);
    newOrder.remove(draggedId);
    int slot = nearestWheelSlot(angleRad, newOrder.size() + 1);
    newOrder.add(slot, draggedId);
    return newOrder;
  }

  static int nearestWheelSlot(double angleRad, int slotCount) {
    if (slotCount <= 0) return 0;
    double fullTurn = Math.PI * 2;
    double normalized = angleRad % fullTurn;
    if (normalized < 0) normalized += fullTurn;
    return (int) Math.floor(normalized / fullTurn * slotCount + 0.5) % slotCount;
  }

  static Vector2d candidateGridTarget(
      int index, int count, double horizontalLimit, double verticalLimit, Vector2d dest) {
    if (index < 0 || index >= count) throw new IllegalArgumentException("Invalid candidate index");

    double nearX = -(GROUP_OUTER_BOUNDARY + CANDIDATE_MARGIN);
    double farX = -horizontalLimit + CANDIDATE_MARGIN;
    double availableWidth = Math.max(nearX - farX, 0);
    double availableHeight = Math.max(verticalLimit * 2 - CANDIDATE_MARGIN * 2, 0);
    int maxColumns = Math.max((int) Math.floor(availableWidth / CANDIDATE_SPACING + 1e-9) + 1, 1);
    int maxRows = Math.max((int) Math.floor(availableHeight / CANDIDATE_SPACING + 1e-9) + 1, 1);
    int columns = Math.min((count + maxRows - 1) / maxRows, maxColumns);

    int baseRows = count / columns;
    int longerColumns = count % columns;
    int longerColumnEntries = (baseRows + 1) * longerColumns;
    int column;
    int row;
    if (index < longerColumnEntries) {
      column = index / (baseRows + 1);
      row = index % (baseRows + 1);
    } else {
      int remainingIndex = index - longerColumnEntries;
      column = longerColumns + remainingIndex / baseRows;
      row = remainingIndex % baseRows;
    }

    int rows = baseRows + (longerColumns > 0 ? 1 : 0);
    double verticalSpacing =
        rows < 2 ? 0 : Math.min(CANDIDATE_SPACING, availableHeight / (rows - 1));
    double topY = -(rows - 1) * verticalSpacing * 0.5;
    double x = Math.max(nearX - column * CANDIDATE_SPACING, farX);
    return dest.set(x, topY + row * verticalSpacing);
  }

  private static Group classify(Group oldGroup, double x) {
    if (x < -GROUP_OUTER_BOUNDARY) return Group.CANDIDATE;
    if (x > GROUP_OUTER_BOUNDARY) return Group.DISABLED;
    if (oldGroup == Group.CANDIDATE && x < -GROUP_INNER_BOUNDARY) return Group.CANDIDATE;
    if (oldGroup == Group.DISABLED && x > GROUP_INNER_BOUNDARY) return Group.DISABLED;
    return Group.SELECTED;
  }

  private static double wheelAngle(PerspectiveActor actor) {
    Vector2d position = actor.body().position();
    double angle = Math.atan2(position.x, -position.y);
    return angle < 0 ? angle + Math.PI * 2 : angle;
  }

  private void render(@NonNull GuiRenderContext context) {
    if (!opened) return;
    if (!isOwnerActive()) {
      close();
      return;
    }
    screenWidth = Math.max(context.screenWidth, 1);
    screenHeight = Math.max(context.screenHeight, 1);
    minEdge = Math.min(screenWidth, screenHeight);
    updateMouseWorld();

    long now = System.nanoTime();
    double frameSeconds = lastRenderNanos == 0 ? 0 : (now - lastRenderNanos) * 1e-9;
    if (frameSeconds > 0) world.advance(frameSeconds);
    lastRenderNanos = now;
    if (mode == Mode.SELECTING) updateSelectingHover();
    else {
      updateEditingHover();
      Component sponsorText = Component.translatable(OrbitMenuRenderer.SPONSOR_TEXT_KEY);
      sponsorButton.update(
          screenWidth,
          screenHeight,
          Minecraft.getInstance().font.width(sponsorText),
          Minecraft.getInstance().font.lineHeight,
          mouseScreen.x,
          mouseScreen.y,
          frameSeconds);
    }
    renderer.render(context, this, frameSeconds);
  }

  private void updateMouseWorld() {
    mouseWorld.set(
        (mouseScreen.x - screenWidth * 0.5) / minEdge,
        (mouseScreen.y - screenHeight * 0.5) / minEdge);
  }

  private void applyLayoutForces(PhysicsBody body) {
    if (!(body.identity() instanceof String id) || model.perspective(id) == null) return;
    switch (model.groupOf(id)) {
      case SELECTED -> applySelectedForce(body, id);
      case CANDIDATE -> applyCandidateForce(body, id);
      case DISABLED -> applyDisabledBoundary(body);
    }
    applyScreenBoundary(body);
  }

  private void applySelectedForce(PhysicsBody body, String id) {
    List<String> selected = model.selected();
    int index = selected.indexOf(id);
    if (index < 0 || selected.isEmpty()) return;
    double angle = Math.PI * 2 * index / selected.size() - Math.PI / 2;
    addSpring(
        body,
        RING_RADIUS * Math.cos(angle),
        RING_RADIUS * Math.sin(angle),
        360,
        LAYOUT_SPRING_MAX_FORCE);
  }

  private void applyCandidateForce(PhysicsBody body, String id) {
    List<String> candidates = model.candidates();
    int index = candidates.indexOf(id);
    if (index < 0) return;
    candidateGridTarget(
        index, candidates.size(), horizontalLimit(), verticalLimit(), candidateTarget);
    addSpring(body, candidateTarget.x, candidateTarget.y, 360, LAYOUT_SPRING_MAX_FORCE);
  }

  private void applyDisabledBoundary(PhysicsBody body) {
    double minX = GROUP_OUTER_BOUNDARY + 0.02;
    double maxX = horizontalLimit() - 0.06;
    double centerX = (minX + maxX) * 0.5;
    addSpring(body, centerX, 0, 72, 24);
    if (body.position().x < minX) addSpring(body, minX, body.position().y, 360, 80);
    else if (body.position().x > maxX) addSpring(body, maxX, body.position().y, 360, 80);
  }

  private void applyDisabledRepulsion(PhysicsBody first, PhysicsBody second) {
    if (!(first.identity() instanceof String firstId)
        || !(second.identity() instanceof String secondId)) return;
    if (model.groupOf(firstId) != Group.DISABLED || model.groupOf(secondId) != Group.DISABLED) {
      return;
    }
    if (first.position().distanceSquared(second.position()) < 1e-20) {
      double forceX = firstId.compareTo(secondId) < 0 ? -10 : 10;
      if (first.type() == BodyType.DYNAMIC) first.addForce(forceX, 0);
      if (second.type() == BodyType.DYNAMIC) second.addForce(-forceX, 0);
      return;
    }
    DISABLED_REPULSION.apply(first, second);
  }

  private void applyScreenBoundary(PhysicsBody body) {
    double maxX = horizontalLimit() - 0.04;
    double maxY = verticalLimit() - 0.04;
    double targetX = Math.max(-maxX, Math.min(maxX, body.position().x));
    double targetY = Math.max(-maxY, Math.min(maxY, body.position().y));
    if (targetX != body.position().x || targetY != body.position().y) {
      addSpring(body, targetX, targetY, 400, 100);
    }
  }

  private static void addSpring(
      PhysicsBody body, double targetX, double targetY, double spring, double maxForce) {
    double criticalDamping = 2 * Math.sqrt(spring * body.mass());
    double springDamping = Math.max(criticalDamping - LINEAR_DRAG_FACTOR, 0);
    double fx = (targetX - body.position().x) * spring - body.velocity().x * springDamping;
    double fy = (targetY - body.position().y) * spring - body.velocity().y * springDamping;
    double lengthSquared = fx * fx + fy * fy;
    if (lengthSquared > maxForce * maxForce) {
      double scale = maxForce / Math.sqrt(lengthSquared);
      fx *= scale;
      fy *= scale;
    }
    body.addForce(fx, fy);
  }

  private static void applyFriction(PhysicsBody body) {
    if (body.type() != BodyType.DYNAMIC) return;
    if (body.velocity().lengthSquared() < 0.0025 && body.totalForce().lengthSquared() < 0.36) {
      body.velocity().zero();
      body.clearForce();
    }
  }

  private void placeInitially(PerspectiveActor actor, Group group) {
    int hash = actor.perspectiveId().hashCode();
    double jitterX = (((hash & 0xff) / 255.0) - 0.5) * 0.04;
    double jitterY = ((((hash >>> 8) & 0xff) / 255.0) - 0.5) * 0.04;
    switch (group) {
      case SELECTED -> {
        List<String> selected = model.selected();
        int index = selected.indexOf(actor.perspectiveId());
        double angle = Math.PI * 2 * index / selected.size() - Math.PI / 2;
        actor
            .body()
            .position()
            .set(
                RING_RADIUS * Math.cos(angle) + jitterX * 0.1,
                RING_RADIUS * Math.sin(angle) + jitterY * 0.1);
      }
      case CANDIDATE -> {
        List<String> candidates = model.candidates();
        int index = candidates.indexOf(actor.perspectiveId());
        actor
            .body()
            .position()
            .set(-0.42 + jitterX * 0.1, (index - (candidates.size() - 1) * 0.5) * 0.1);
      }
      case DISABLED -> actor.body().position().set(0.42 + jitterX, jitterY);
    }
  }

  private boolean isAvailable(PerspectiveActor actor) {
    Perspective perspective = model.perspective(actor.perspectiveId());
    return perspective != null && perspective.isAvailable();
  }

  private boolean isOwnerActive() {
    return PerspectiveAPI.isEnabled()
        && PerspectiveAPI.getSwitcherManager().getSelectedSwitcher() == owner;
  }

  private static boolean hasOpenScreen() {
    return Bridge.getScreen(Minecraft.getInstance()) != null;
  }

  private double horizontalLimit() {
    return screenWidth / minEdge * 0.5;
  }

  private double verticalLimit() {
    return screenHeight / minEdge * 0.5;
  }
}
