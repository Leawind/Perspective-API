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
                          .optionalFieldOf("currentPerspective")
                          .forGetter(s -> Optional.ofNullable(s.currentPerspective)),
                      Identifier.CODEC
                          .optionalFieldOf("activeCyclerPerspective")
                          .forGetter(s -> Optional.ofNullable(s.activeCyclerPerspective)))
                  .apply(inst, PerspectiveApiState::new));

  private final boolean enabled;
  private final @Nullable Identifier currentPerspective;
  private final @Nullable Identifier activeCyclerPerspective;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private PerspectiveApiState(
      boolean enabled,
      Optional<Identifier> currentPerspective,
      Optional<Identifier> activeCyclerPerspective) {

    this.enabled = enabled;
    this.currentPerspective = currentPerspective.orElse(null);
    this.activeCyclerPerspective = activeCyclerPerspective.orElse(null);
  }

  // endregion

  // region state <=> Perspective API

  public void apply() {
    PerspectiveAPI.setEnabled(enabled);

    if (currentPerspective != null) {
      PerspectiveManagerImpl.INSTANCE.setCurrentId(currentPerspective);
    }

    PerspectiveAPI.getManager().cycler().setActive(activeCyclerPerspective);
  }

  public static PerspectiveApiState extract() {
    return new PerspectiveApiState(
        PerspectiveAPI.isEnabled(),
        Optional.of(PerspectiveManagerImpl.INSTANCE.getCurrent().id()),
        Optional.ofNullable(PerspectiveAPI.getManager().cycler().getActive()));
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
