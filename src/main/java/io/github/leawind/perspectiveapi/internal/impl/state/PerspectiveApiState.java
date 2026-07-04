package io.github.leawind.perspectiveapi.internal.impl.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveManagerImpl;
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
                      Identifier.CODEC
                          .optionalFieldOf("cycler.active")
                          .forGetter(s -> Optional.ofNullable(s.cyclerActive)))
                  .apply(inst, PerspectiveApiState::new));

  private final boolean enabled;
  private final @Nullable Identifier managerCurrent;
  private final @Nullable Identifier cyclerActive;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private PerspectiveApiState(
      boolean enabled, Optional<Identifier> managerCurrent, Optional<Identifier> cyclerActive) {

    this.enabled = enabled;
    this.managerCurrent = managerCurrent.orElse(null);
    this.cyclerActive = cyclerActive.orElse(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PerspectiveApiState that)) return false;
    return enabled == that.enabled
        && Objects.equals(managerCurrent, that.managerCurrent)
        && Objects.equals(cyclerActive, that.cyclerActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, managerCurrent, cyclerActive);
  }

  // endregion

  // region state <=> Perspective API

  public void apply() {
    PerspectiveAPI.setEnabled(enabled);

    if (managerCurrent != null) {
      PerspectiveManagerImpl.INSTANCE.setCurrentId(managerCurrent);
    }

    PerspectiveAPI.getManager().cycler().setActiveId(cyclerActive);
  }

  public static PerspectiveApiState extract() {
    return new PerspectiveApiState(
        PerspectiveAPI.isEnabled(),
        Optional.of(PerspectiveManagerImpl.INSTANCE.getCurrent().id()),
        Optional.ofNullable(PerspectiveAPI.getManager().cycler().getActiveId()));
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
