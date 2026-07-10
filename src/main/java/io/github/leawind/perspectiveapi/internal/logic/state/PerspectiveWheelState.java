package io.github.leawind.perspectiveapi.internal.logic.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import io.github.leawind.perspectiveapi.internal.logic.builtin.VanillaPerspective;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class PerspectiveWheelState {

  private static final Codec<Set<Identifier>> ID_SET_CODEC =
      Identifier.CODEC.listOf().xmap(HashSet::new, ArrayList::new);

  public static final Codec<PerspectiveWheelState> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      Identifier.CODEC
                          .optionalFieldOf("active")
                          .forGetter(w -> Optional.ofNullable(w.activeId)),
                      Identifier.CODEC
                          .listOf()
                          .optionalFieldOf("selected")
                          .forGetter(w -> Optional.ofNullable(w.selected)),
                      ID_SET_CODEC
                          .optionalFieldOf("disabled")
                          .forGetter(w -> Optional.ofNullable(w.disabled)))
                  .apply(inst, PerspectiveWheelState::new));

  private final @Nullable Identifier activeId;
  private final List<Identifier> selected;
  private final Set<Identifier> disabled;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private PerspectiveWheelState(
      Optional<Identifier> activeId,
      Optional<List<Identifier>> selected,
      Optional<Set<Identifier>> disabled) {
    this.activeId = activeId.orElse(null);
    this.selected =
        selected.orElseGet(
            () ->
                new ArrayList<>(
                    List.of(
                        VanillaPerspective.FIRST_PERSON.id(),
                        VanillaPerspective.THIRD_PERSON_BACK.id(),
                        VanillaPerspective.THIRD_PERSON_FRONT.id())));
    this.disabled = disabled.orElseGet(HashSet::new);
  }

  public void apply() {
    PerspectiveManager.INSTANCE.wheel().restore(activeId, selected, disabled);
  }

  public static @NonNull PerspectiveWheelState extract() {
    var wheel = PerspectiveManager.INSTANCE.wheel();
    return new PerspectiveWheelState(
        Optional.ofNullable(wheel.get()),
        Optional.of(wheel.getSelected()),
        Optional.of(wheel.getDisabled()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(activeId, selected, disabled);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PerspectiveWheelState that)) return false;
    return Objects.equals(activeId, that.activeId)
        && Objects.equals(selected, that.selected)
        && Objects.equals(disabled, that.disabled);
  }
}
