package io.github.leawind.perspectiveapi.internal.logic.state;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import io.github.leawind.perspectiveapi.internal.impl.PerspectiveRegistryImpl;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerspectiveAPIState {
  /// A self-contained section of Perspective API's persisted state.
  ///
  /// Implementations own their state schema and must validate a decoded value before mutating
  /// runtime state in {@link #applyState(Object)}.
  public interface Section<T> {
    /// Returns the stable key used below the root `sections` object.
    @NonNull String stateId();

    /// Returns the codec for this section's value.
    @NonNull Codec<T> stateCodec();

    /// Captures the current runtime state.
    @NonNull T extractState();

    /// Applies previously persisted state.
    ///
    /// @param state the decoded and validated state
    void applyState(@NonNull T state);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveAPIState.class);
  private static final Codec<Map<String, Dynamic<?>>> SECTIONS_CODEC =
      Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH);
  private static final Map<String, Section<?>> REGISTERED_SECTIONS = new TreeMap<>();

  private static final Codec<PerspectiveAPIState> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Codec.BOOL.optionalFieldOf("enabled", true).forGetter((s) -> s.enabled),
                      Codec.INT
                          .optionalFieldOf(
                              "logic_tick_interval", PerspectiveAPI.DEFAULT_LOGIC_TICK_INTERVAL)
                          .forGetter(s -> s.logicTickInterval),
                      Codec.STRING
                          .optionalFieldOf("manager.current")
                          .forGetter(s -> Optional.ofNullable(s.managerCurrent)),
                      Codec.STRING
                          .optionalFieldOf("manager.switcher")
                          .forGetter(s -> Optional.ofNullable(s.managerSwitcher)),
                      Codec.DOUBLE
                          .optionalFieldOf("transition.duration_ms", 300.0)
                          .forGetter(s -> s.transitionDurationMs),
                      Codec.DOUBLE
                          .optionalFieldOf("transition.blend_power", 1.0)
                          .forGetter(s -> s.transitionBlendPower),
                      SECTIONS_CODEC
                          .optionalFieldOf("sections", Map.of())
                          .forGetter(s -> s.sections))
                  .apply(inst, PerspectiveAPIState::new));

  private final boolean enabled;
  private final int logicTickInterval;
  private final @Nullable String managerCurrent;
  private final @Nullable String managerSwitcher;
  private final double transitionDurationMs;
  private final double transitionBlendPower;
  private final Map<String, Dynamic<?>> sections;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private PerspectiveAPIState(
      boolean enabled,
      int logicTickInterval,
      Optional<String> managerCurrent,
      Optional<String> managerSwitcher,
      double transitionDurationMs,
      double transitionBlendPower,
      Map<String, Dynamic<?>> sections) {
    this.enabled = enabled;
    this.logicTickInterval = logicTickInterval;
    this.managerCurrent = managerCurrent.orElse(null);
    this.managerSwitcher = managerSwitcher.orElse(null);
    this.transitionDurationMs = transitionDurationMs;
    this.transitionBlendPower = transitionBlendPower;
    this.sections = Collections.unmodifiableMap(new TreeMap<>(sections));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PerspectiveAPIState that)) return false;
    return enabled == that.enabled
        && logicTickInterval == that.logicTickInterval
        && Double.compare(that.transitionDurationMs, transitionDurationMs) == 0
        && Double.compare(that.transitionBlendPower, transitionBlendPower) == 0
        && Objects.equals(managerCurrent, that.managerCurrent)
        && Objects.equals(managerSwitcher, that.managerSwitcher)
        && sections.equals(that.sections);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        enabled,
        logicTickInterval,
        managerCurrent,
        managerSwitcher,
        transitionDurationMs,
        transitionBlendPower,
        sections);
  }

  public void apply() {
    if (logicTickInterval < 1) {
      throw new IllegalArgumentException("logic_tick_interval must be at least 1");
    }
    if (!Double.isFinite(transitionDurationMs) || transitionDurationMs < 0) {
      throw new IllegalArgumentException("transition.duration_ms must be finite and non-negative");
    }
    if (!Double.isFinite(transitionBlendPower) || transitionBlendPower <= 0) {
      throw new IllegalArgumentException("transition.blend_power must be finite and positive");
    }

    PerspectiveAPI.setEnabled(enabled);
    PerspectiveAPI.setLogicTickInterval(logicTickInterval);

    if (managerCurrent != null) {
      Perspective perspective = PerspectiveRegistryImpl.INSTANCE.getOrDefault(managerCurrent);
      PerspectiveManager.INSTANCE.setCurrent(perspective);
    }

    if (managerSwitcher != null) {
      PerspectiveSwitcherBehavior switcher =
          PerspectiveManager.INSTANCE.switchers().getById(managerSwitcher);
      if (switcher != null) {
        PerspectiveManager.INSTANCE.switchers().setSelectedSwitcher(switcher);
      }
    }

    PerspectiveAPI.getTransition().setDurationMs(transitionDurationMs);
    PerspectiveAPI.getTransition().setBlendPower(transitionBlendPower);
    applySections(sections);
  }

  static PerspectiveAPIState extract(@Nullable PerspectiveAPIState existing) {
    Map<String, Dynamic<?>> existingSections = existing == null ? Map.of() : existing.sections;
    return new PerspectiveAPIState(
        PerspectiveAPI.isEnabled(),
        PerspectiveAPI.getLogicTickInterval(),
        Optional.of(PerspectiveManager.INSTANCE.getCurrent().info().id()),
        Optional.of(PerspectiveAPI.getSwitcherManager().getSelectedSwitcher().id()),
        PerspectiveAPI.getTransition().getDurationMs(),
        PerspectiveAPI.getTransition().getBlendPower(),
        extractSections(existingSections));
  }

  public static synchronized void registerSection(@NonNull Section<?> section) {
    Objects.requireNonNull(section);
    String id = Objects.requireNonNull(section.stateId());
    if (id.isEmpty()) throw new IllegalArgumentException("State section id must not be empty");
    if (REGISTERED_SECTIONS.putIfAbsent(id, section) != null) {
      throw new IllegalArgumentException("State section id is already registered: '" + id + "'");
    }
  }

  private static void applySections(@NonNull Map<String, Dynamic<?>> states) {
    for (Section<?> section : registeredSections()) {
      Dynamic<?> state = states.get(section.stateId());
      if (state == null) continue;
      try {
        applySection(section, state);
      } catch (Exception e) {
        LOGGER.warn("Failed to apply state section '{}', skipping it", section.stateId(), e);
      }
    }
  }

  private static @NonNull Map<String, Dynamic<?>> extractSections(
      @NonNull Map<String, Dynamic<?>> existing) {
    Map<String, Dynamic<?>> states = new TreeMap<>(existing);
    for (Section<?> section : registeredSections()) {
      try {
        states.put(section.stateId(), encodeSection(section));
      } catch (Exception e) {
        LOGGER.warn(
            "Failed to extract state section '{}', preserving its existing value",
            section.stateId(),
            e);
      }
    }
    return Collections.unmodifiableMap(states);
  }

  private static synchronized Section<?>[] registeredSections() {
    return REGISTERED_SECTIONS.values().toArray(Section<?>[]::new);
  }

  private static <T> void applySection(@NonNull Section<T> section, @NonNull Dynamic<?> state) {
    applySectionDynamic(section, state);
  }

  private static <T, U> void applySectionDynamic(
      @NonNull Section<T> section, @NonNull Dynamic<U> state) {
    T decoded =
        section
            .stateCodec()
            .parse(state.getOps(), state.getValue())
            .result()
            .orElseThrow(
                () ->
                    new JsonSyntaxException("Failed to decode state section " + section.stateId()));
    section.applyState(decoded);
  }

  private static <T> @NonNull Dynamic<?> encodeSection(@NonNull Section<T> section) {
    JsonElement json =
        section
            .stateCodec()
            .encodeStart(JsonOps.INSTANCE, section.extractState())
            .result()
            .orElseThrow(
                () ->
                    new JsonSyntaxException("Failed to encode state section " + section.stateId()));
    return new Dynamic<>(JsonOps.INSTANCE, json);
  }

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  public void save(Path path) throws IOException {
    JsonElement jsonElement =
        CODEC
            .encodeStart(JsonOps.INSTANCE, this)
            .result()
            .orElseThrow(() -> new JsonSyntaxException("Failed to encode Perspective API state"));
    Files.writeString(path, GSON.toJson(jsonElement));
  }

  public static PerspectiveAPIState load(Path path) throws IOException, JsonSyntaxException {
    JsonElement json = GSON.fromJson(Files.readString(path), JsonElement.class);
    return CODEC
        .parse(JsonOps.INSTANCE, json)
        .result()
        .orElseThrow(() -> new JsonSyntaxException("Failed to decode Perspective API state"));
  }
}
