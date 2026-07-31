package io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;

/// Persisted player configuration for the orbit switcher.
public record OrbitSwitcherState(
    @NonNull List<@NonNull String> selected, @NonNull Set<@NonNull String> disabled) {
  private static final Codec<Set<String>> STRING_SET_CODEC =
      Codec.STRING
          .listOf()
          .xmap(Set::copyOf, values -> values.stream().sorted().toList());

  static final Codec<OrbitSwitcherState> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.STRING
                          .listOf()
                          .optionalFieldOf("selected", List.of())
                          .forGetter(OrbitSwitcherState::selected),
                      STRING_SET_CODEC
                          .optionalFieldOf("disabled", Set.of())
                          .forGetter(OrbitSwitcherState::disabled))
                  .apply(instance, OrbitSwitcherState::new));

  public OrbitSwitcherState {
    Objects.requireNonNull(selected);
    Objects.requireNonNull(disabled);
    selected.forEach(Objects::requireNonNull);
    disabled.forEach(Objects::requireNonNull);
    selected = List.copyOf(selected);
    disabled = Set.copyOf(disabled);
  }
}
