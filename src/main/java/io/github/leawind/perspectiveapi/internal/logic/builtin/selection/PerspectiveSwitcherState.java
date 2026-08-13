package io.github.leawind.perspectiveapi.internal.logic.builtin.selection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/// Persisted player configuration for the built-in perspective switcher.
public record PerspectiveSwitcherState(
    @NonNull List<@NonNull String> selected,
    @NonNull Set<@NonNull String> disabled,
    int holdTicks,
    boolean wheelHintCompleted,
    boolean editorHintCompleted) {
  private static final Codec<Set<String>> STRING_SET_CODEC =
      Codec.STRING.listOf().xmap(Set::copyOf, values -> values.stream().sorted().toList());

  static final Codec<PerspectiveSwitcherState> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.STRING
                          .listOf()
                          .optionalFieldOf("selected", List.of())
                          .forGetter(PerspectiveSwitcherState::selected),
                      STRING_SET_CODEC
                          .optionalFieldOf("disabled", Set.of())
                          .forGetter(PerspectiveSwitcherState::disabled),
                      Codec.intRange(
                              PerspectiveSwitcher.MIN_HOLD_TICKS,
                              PerspectiveSwitcher.MAX_HOLD_TICKS)
                          .optionalFieldOf("hold_ticks", PerspectiveSwitcher.DEFAULT_HOLD_TICKS)
                          .forGetter(PerspectiveSwitcherState::holdTicks),
                      Codec.BOOL
                          .optionalFieldOf("wheel_hint_completed", false)
                          .forGetter(PerspectiveSwitcherState::wheelHintCompleted),
                      Codec.BOOL
                          .optionalFieldOf("editor_hint_completed", false)
                          .forGetter(PerspectiveSwitcherState::editorHintCompleted))
                  .apply(instance, PerspectiveSwitcherState::new));

  public PerspectiveSwitcherState {
    Objects.requireNonNull(selected);
    Objects.requireNonNull(disabled);
    selected.forEach(Objects::requireNonNull);
    disabled.forEach(Objects::requireNonNull);
    PerspectiveSwitcher.validateHoldTicks(holdTicks);
    selected = List.copyOf(selected);
    disabled = Set.copyOf(disabled);
  }
}
