package io.github.leawind.perspectiveapi.internal.logic.state;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.impl.TransitionImpl;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PerspectiveAPIStateTest {

  @AfterEach
  void afterEach() {
    PerspectiveAPI.setEnabled(true);
    PerspectiveManager.INSTANCE.restoreSelection(null);
    PerspectiveManager.INSTANCE.transition().setEnabled(false);
    PerspectiveAPI.getTransition().setDurationMs(TransitionImpl.DEFAULT_DURATION_MS);
  }

  @Test
  void oldStateDefaultsTransitionsToDisabled() {
    PerspectiveAPIState state =
        PerspectiveAPIState.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).result().orElseThrow();

    state.apply();

    assertFalse(PerspectiveManager.INSTANCE.transition().isEnabled());
  }

  @Test
  void transitionEnabledRoundTrips() {
    JsonObject json = new JsonObject();
    json.addProperty("transition.enabled", true);

    PerspectiveAPIState state =
        PerspectiveAPIState.CODEC.parse(JsonOps.INSTANCE, json).result().orElseThrow();
    state.apply();
    assertTrue(PerspectiveManager.INSTANCE.transition().isEnabled());

    var encoded =
        PerspectiveAPIState.CODEC
            .encodeStart(JsonOps.INSTANCE, PerspectiveAPIState.extract(null))
            .result()
            .orElseThrow();
    assertTrue(encoded.getAsJsonObject().get("transition.enabled").getAsBoolean());
  }
}
