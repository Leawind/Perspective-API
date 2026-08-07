package io.github.leawind.perspectiveapi.internal.logic.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.CyclingListControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.internal.impl.TransitionImpl;
import io.github.leawind.perspectiveapi.internal.impl.transition.TransitionAlgorithms;
import io.github.leawind.perspectiveapi.internal.impl.transition.position.PositionTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.rotation.RotationTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.FovTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.impl.transition.scalar.OrthographicHeightTransitionAlgorithm;
import io.github.leawind.perspectiveapi.internal.logic.PerspectiveManager;
import io.github.leawind.perspectiveapi.internal.logic.builtin.switchers.orbit.OrbitSwitcherBehavior;
import io.github.leawind.perspectiveapi.platform.api.Services;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class YaclConfigScreenBuilder {

  public static Screen build(Screen parent) {
    var builder =
        YetAnotherConfigLib.createBuilder()
            .title(text("config_screen.title"))
            .category(
                ConfigCategory.createBuilder()
                    .name(text("config_screen.title"))
                    .option(
                        Option.<Boolean>createBuilder()
                            .name(text("config_screen.option.mod_enabled"))
                            .description(
                                OptionDescription.of(text("config_screen.option.mod_enabled.desc")))
                            .binding(
                                PerspectiveAPI.isEnabled(),
                                PerspectiveAPI::isEnabled,
                                PerspectiveAPI::setEnabled)
                            .controller(TickBoxControllerBuilder::create)
                            .build())
                    .group(buildTransitionGroup())
                    .option(
                        Option.<PerspectiveSwitcher>createBuilder()
                            .name(text("config_screen.option.switcher"))
                            .description(
                                switcher -> {
                                  Component desc = switcher.description();
                                  if (desc == null) return OptionDescription.EMPTY;
                                  return OptionDescription.of(desc);
                                })
                            .binding(
                                PerspectiveAPI.getSwitcherManager().getSelectedSwitcher(),
                                () -> PerspectiveAPI.getSwitcherManager().getSelectedSwitcher(),
                                switcher ->
                                    PerspectiveAPI.getSwitcherManager()
                                        .setSelectedSwitcher(switcher))
                            .controller(
                                opt ->
                                    CyclingListControllerBuilder.create(opt)
                                        .values(
                                            PerspectiveAPI.getSwitcherManager()
                                                .getAvailableSwitchers())
                                        .formatValue(PerspectiveSwitcher::name))
                            .build())
                    .group(
                        OptionGroup.createBuilder()
                            .name(text("config_screen.group.orbit_switcher"))
                            .option(
                                Option.<Integer>createBuilder()
                                    .name(text("config_screen.option.orbit_switcher_hold_ticks"))
                                    .description(
                                        OptionDescription.of(
                                            text(
                                                "config_screen.option.orbit_switcher_hold_ticks.desc")))
                                    .binding(
                                        OrbitSwitcherBehavior.DEFAULT_HOLD_TICKS,
                                        OrbitSwitcherBehavior.INSTANCE::getHoldTicks,
                                        OrbitSwitcherBehavior.INSTANCE::setHoldTicks)
                                    .controller(
                                        opt ->
                                            IntegerSliderControllerBuilder.create(opt)
                                                .range(
                                                    OrbitSwitcherBehavior.MIN_HOLD_TICKS,
                                                    OrbitSwitcherBehavior.MAX_HOLD_TICKS)
                                                .step(1)
                                                .formatValue(
                                                    v ->
                                                        Component.translatable(
                                                            PerspectiveAPI.MOD_ID
                                                                + ".config_screen.option.orbit_switcher_hold_ticks.value",
                                                            v)))
                                    .build())
                            .build())
                    .build());
    if (Services.PLATFORM_HELPER.isDevelopmentEnvironment()) {
      builder.category(buildDebugCategory());
    }
    return builder.build().generateScreen(parent);
  }

  private static ConfigCategory buildDebugCategory() {
    TransitionImpl transition = PerspectiveManager.INSTANCE.transition();
    return ConfigCategory.createBuilder()
        .name(text("config_screen.category.debug"))
        .option(
            Option.<PositionTransitionAlgorithm>createBuilder()
                .name(text("config_screen.option.position_transition_algorithm"))
                .description(
                    OptionDescription.of(text("config_screen.option.transition_algorithm.desc")))
                .binding(
                    TransitionImpl.DEFAULT_POSITION_ALGORITHM,
                    transition::getPositionAlgorithm,
                    transition::setPositionAlgorithm)
                .controller(
                    opt ->
                        CyclingListControllerBuilder.create(opt)
                            .values(TransitionAlgorithms.POSITION)
                            .formatValue(
                                algorithm ->
                                    text(
                                        "config_screen.option.position_transition_algorithm."
                                            + algorithm.id())))
                .build())
        .option(
            Option.<RotationTransitionAlgorithm>createBuilder()
                .name(text("config_screen.option.rotation_transition_algorithm"))
                .description(
                    OptionDescription.of(text("config_screen.option.transition_algorithm.desc")))
                .binding(
                    TransitionImpl.DEFAULT_ROTATION_ALGORITHM,
                    transition::getRotationAlgorithm,
                    transition::setRotationAlgorithm)
                .controller(
                    opt ->
                        CyclingListControllerBuilder.create(opt)
                            .values(TransitionAlgorithms.ROTATION)
                            .formatValue(
                                algorithm ->
                                    text(
                                        "config_screen.option.rotation_transition_algorithm."
                                            + algorithm.id())))
                .build())
        .option(
            Option.<FovTransitionAlgorithm>createBuilder()
                .name(text("config_screen.option.fov_transition_algorithm"))
                .description(
                    OptionDescription.of(text("config_screen.option.transition_algorithm.desc")))
                .binding(
                    TransitionImpl.DEFAULT_FOV_ALGORITHM,
                    transition::getFovAlgorithm,
                    transition::setFovAlgorithm)
                .controller(
                    opt ->
                        CyclingListControllerBuilder.create(opt)
                            .values(TransitionAlgorithms.FOV)
                            .formatValue(
                                algorithm ->
                                    text(
                                        "config_screen.option.scalar_transition_algorithm."
                                            + algorithm.id())))
                .build())
        .option(
            Option.<OrthographicHeightTransitionAlgorithm>createBuilder()
                .name(text("config_screen.option.orthographic_height_transition_algorithm"))
                .description(
                    OptionDescription.of(text("config_screen.option.transition_algorithm.desc")))
                .binding(
                    TransitionImpl.DEFAULT_ORTHOGRAPHIC_HEIGHT_ALGORITHM,
                    transition::getOrthographicHeightAlgorithm,
                    transition::setOrthographicHeightAlgorithm)
                .controller(
                    opt ->
                        CyclingListControllerBuilder.create(opt)
                            .values(TransitionAlgorithms.ORTHOGRAPHIC_HEIGHT)
                            .formatValue(
                                algorithm ->
                                    text(
                                        "config_screen.option.scalar_transition_algorithm."
                                            + algorithm.id())))
                .build())
        .build();
  }

  private static OptionGroup buildTransitionGroup() {
    OptionGroup.Builder builder =
        OptionGroup.createBuilder()
            .name(text("config_screen.group.transition"))
            .option(
                Option.<Double>createBuilder()
                    .name(text("config_screen.option.transition_duration"))
                    .description(
                        OptionDescription.of(text("config_screen.option.transition_duration.desc")))
                    .binding(
                        TransitionImpl.DEFAULT_DURATION_MS,
                        () -> PerspectiveAPI.getTransition().getDurationMs(),
                        v -> PerspectiveAPI.getTransition().setDurationMs(v))
                    .controller(
                        opt ->
                            DoubleSliderControllerBuilder.create(opt)
                                .range(0.0, 1000.0)
                                .step(20.0)
                                .formatValue(v -> Component.literal(v.intValue() + " ms")))
                    .build());

    return builder.build();
  }

  private static Component text(String key) {
    return Component.translatable(PerspectiveAPI.MOD_ID + "." + key);
  }
}
