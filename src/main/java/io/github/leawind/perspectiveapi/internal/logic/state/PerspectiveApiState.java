package io.github.leawind.perspectiveapi.internal.logic.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class PerspectiveApiState {
  // region codec

  private static final Codec<PerspectiveApiState> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.BOOL.optionalFieldOf("enabled", true).forGetter((s) -> s.enabled),
                      Identifier.CODEC
                          .optionalFieldOf("manager.current")
                          .forGetter(s -> Optional.ofNullable(s.managerCurrent)),
                      PerspectiveWheelState.CODEC
                          .optionalFieldOf("wheel")
                          .forGetter(s -> Optional.ofNullable(s.wheel)),
                      Codec.DOUBLE
                          .optionalFieldOf("transition.duration_ms", 300.0)
                          .forGetter(s -> s.transitionDurationMs))
                  .apply(inst, PerspectiveApiState::new));

  private final boolean enabled;
  private final @Nullable Identifier managerCurrent;
  private final @Nullable PerspectiveWheelState wheel;
  private final double transitionDurationMs;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private PerspectiveApiState(
      boolean enabled,
      Optional<Identifier> managerCurrent,
      Optional<PerspectiveWheelState> wheel,
      double transitionDurationMs) {

    this.enabled = enabled;
    this.managerCurrent = managerCurrent.orElse(null);
    this.wheel = wheel.orElse(null);
    this.transitionDurationMs = transitionDurationMs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PerspectiveApiState that)) return false;
    return enabled == that.enabled
        && Double.compare(that.transitionDurationMs, transitionDurationMs) == 0
        && Objects.equals(managerCurrent, that.managerCurrent)
        && Objects.equals(wheel, that.wheel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, managerCurrent, wheel, transitionDurationMs);
  }

  // endregion

  // region state <=> Perspective API

  public void apply() {
    PerspectiveAPI.setEnabled(enabled);

    if (managerCurrent != null) {
      PerspectiveManager.INSTANCE.setCurrentId(managerCurrent);
    }

    if (wheel != null) {
      wheel.apply();
    }

    PerspectiveAPI.getTransitionController().setDurationMs(transitionDurationMs);
  }

  public static PerspectiveApiState extract() {
    return new PerspectiveApiState(
        PerspectiveAPI.isEnabled(),
        Optional.of(PerspectiveManager.INSTANCE.getCurrent().id()),
        Optional.of(PerspectiveWheelState.extract()),
        PerspectiveAPI.getTransitionController().getDurationMs());
  }

  // endregion

  // region state <=> Disk

  private static final Gson GSON =
      new GsonBuilder()
          .excludeFieldsWithoutExposeAnnotation()
          .setPrettyPrinting()
          .disableHtmlEscaping()
          .create();

  public void save(Path path) throws IOException {
    var jsonElement = CODEC.encodeStart(JsonOps.INSTANCE, this).result().orElseThrow();
    var json = GSON.toJson(jsonElement);
    Files.writeString(path, json);
  }

  public static PerspectiveApiState load(Path path) throws IOException, JsonSyntaxException {
    var json = Files.readString(path);
    JsonElement jsonElement = GSON.fromJson(json, JsonElement.class);
    return CODEC.parse(JsonOps.INSTANCE, jsonElement).result().orElseThrow();
  }

  // endregion
}
