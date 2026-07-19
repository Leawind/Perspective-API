package io.github.leawind.perspectiveapi.internal.logic.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public final class PerspectiveApiState {
  private static final Codec<PerspectiveApiState> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.BOOL.optionalFieldOf("enabled", true).forGetter((s) -> s.enabled),
                      Codec.STRING
                          .optionalFieldOf("manager.current")
                          .forGetter(s -> Optional.ofNullable(s.managerCurrent)),
                      Codec.DOUBLE
                          .optionalFieldOf("transition.duration_ms", 300.0)
                          .forGetter(s -> s.transitionDurationMs),
                      Codec.DOUBLE
                          .optionalFieldOf("transition.blend_power", 1.0)
                          .forGetter(s -> s.transitionBlendPower))
                  .apply(inst, PerspectiveApiState::new));

  private final boolean enabled;
  private final @Nullable String managerCurrent;
  private final double transitionDurationMs;
  private final double transitionBlendPower;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private PerspectiveApiState(
      boolean enabled,
      Optional<String> managerCurrent,
      double transitionDurationMs,
      double transitionBlendPower) {
    this.enabled = enabled;
    this.managerCurrent = managerCurrent.orElse(null);
    this.transitionDurationMs = transitionDurationMs;
    this.transitionBlendPower = transitionBlendPower;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PerspectiveApiState that)) return false;
    return enabled == that.enabled
        && Double.compare(that.transitionDurationMs, transitionDurationMs) == 0
        && Double.compare(that.transitionBlendPower, transitionBlendPower) == 0
        && Objects.equals(managerCurrent, that.managerCurrent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, managerCurrent, transitionDurationMs, transitionBlendPower);
  }

  public void apply() {
    PerspectiveAPI.setEnabled(enabled);

    if (managerCurrent != null) {
      Perspective perspective = PerspectiveRegistryImpl.INSTANCE.getOrDefault(managerCurrent);
      PerspectiveManager.INSTANCE.setCurrent(perspective);
    }

    PerspectiveAPI.getTransition().setDurationMs(transitionDurationMs);
    PerspectiveAPI.getTransition().setBlendPower(transitionBlendPower);
  }

  public static PerspectiveApiState extract() {
    return new PerspectiveApiState(
        PerspectiveAPI.isEnabled(),
        Optional.of(PerspectiveManager.INSTANCE.getCurrent().id()),
        PerspectiveAPI.getTransition().getDurationMs(),
        PerspectiveAPI.getTransition().getBlendPower());
  }

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
}
