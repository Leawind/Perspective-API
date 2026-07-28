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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class YaclConfigScreenBuilder {

  public static Screen build(Screen parent) {
    return YetAnotherConfigLib.createBuilder()
        .title(text("config_screen.title"))
        .category(
            ConfigCategory.createBuilder()
                .name(text("config_screen.category.general"))
                .tooltip(text("config_screen.category.general.desc"))
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
                .option(
                    Option.<Integer>createBuilder()
                        .name(text("config_screen.option.logic_tick_interval"))
                        .description(
                            OptionDescription.of(
                                text("config_screen.option.logic_tick_interval.desc")))
                        .binding(
                            PerspectiveAPI.DEFAULT_LOGIC_TICK_INTERVAL,
                            PerspectiveAPI::getLogicTickInterval,
                            PerspectiveAPI::setLogicTickInterval)
                        .controller(
                            opt ->
                                IntegerSliderControllerBuilder.create(opt)
                                    .range(1, 3 )
                                    .step(1)
                                    .formatValue(
                                        v ->
                                            Component.translatable(
                                                PerspectiveAPI.MOD_ID
                                                    + ".config_screen.option.logic_tick_interval.value",
                                                v)))
                        .build())
                .group(
                    OptionGroup.createBuilder()
                        .name(text("config_screen.group.transition"))
                        .option(
                            Option.<Double>createBuilder()
                                .name(text("config_screen.option.transition_duration"))
                                .description(
                                    OptionDescription.of(
                                        text("config_screen.option.transition_duration.desc")))
                                .binding(
                                    260.0,
                                    () -> PerspectiveAPI.getTransition().getDurationMs(),
                                    v -> PerspectiveAPI.getTransition().setDurationMs(v))
                                .controller(
                                    opt ->
                                        DoubleSliderControllerBuilder.create(opt)
                                            .range(0.0, 800.0)
                                            .step(20.0)
                                            .formatValue(
                                                v -> Component.literal(v.intValue() + " ms")))
                                .build())
                        .option(
                            Option.<Double>createBuilder()
                                .name(text("config_screen.option.blend_power"))
                                .description(
                                    OptionDescription.of(
                                        text("config_screen.option.blend_power.desc")))
                                .binding(
                                    0.6,
                                    PerspectiveAPI.getTransition()::getBlendPower,
                                    PerspectiveAPI.getTransition()::setBlendPower)
                                .controller(
                                    opt ->
                                        DoubleSliderControllerBuilder.create(opt)
                                            .range(0.1, 4.0)
                                            .step(0.1)
                                            .formatValue(
                                                v -> Component.literal(String.format("%.1f", v))))
                                .build())
                        .build())
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
                                PerspectiveAPI.getSwitcherManager().setSelectedSwitcher(switcher))
                        .controller(
                            opt ->
                                CyclingListControllerBuilder.create(opt)
                                    .values(
                                        PerspectiveAPI.getSwitcherManager().getAvailableSwitchers())
                                    .formatValue(PerspectiveSwitcher::name))
                        .build())
                .build())
        .build()
        .generateScreen(parent);
  }

  private static Component text(String key) {
    return Component.translatable(PerspectiveAPI.MOD_ID + "." + key);
  }
}
