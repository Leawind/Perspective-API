package io.github.leawind.perspectiveapi.internal.logic.config;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.internal.bridge.Bridge;
import java.util.stream.Collectors;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class YaclConfigScreenBuilder {

  public static Screen build(Screen parent) {
    var cycler = PerspectiveAPI.getCycler();

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
                    Option.<Double>createBuilder()
                        .name(text("config_screen.option.transition_duration"))
                        .description(
                            OptionDescription.of(
                                text("config_screen.option.transition_duration.desc")))
                        .binding(
                            PerspectiveAPI.getTransitionController().getDurationMs(),
                            () -> PerspectiveAPI.getTransitionController().getDurationMs(),
                            v -> PerspectiveAPI.getTransitionController().setDurationMs(v))
                        .controller(
                            opt ->
                                DoubleSliderControllerBuilder.create(opt)
                                    .range(0.0, 2048.0)
                                    .step(32.0)
                                    .formatValue(v -> Component.literal(v.intValue() + " ms")))
                        .build())
                .build())
        .category(
            ConfigCategory.createBuilder()
                .name(text("config_screen.category.cycler"))
                .tooltip(text("config_screen.category.cycler.desc"))
                .option(
                    Option.<Boolean>createBuilder()
                        .name(text("config_screen.option.cycler_custom_order"))
                        .description(
                            OptionDescription.of(
                                text("config_screen.option.cycler_custom_order.desc")))
                        .binding(
                            cycler.isCustomOrderEnabled(),
                            cycler::isCustomOrderEnabled,
                            cycler::setCustomOrderEnabled)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .group(
                    ListOption.<String>createBuilder()
                        .name(text("config_screen.option.cycler_custom_order_list"))
                        .description(
                            OptionDescription.of(
                                text("config_screen.option.cycler_custom_order_list.desc")))
                        .binding(
                            cycler.getCustomOrder().stream()
                                .map(Identifier::toString)
                                .collect(Collectors.toList()),
                            () ->
                                cycler.getCustomOrder().stream()
                                    .map(Identifier::toString)
                                    .collect(Collectors.toList()),
                            v ->
                                cycler.setCustomOrder(
                                    v.stream()
                                        .map(Bridge::parseIdentifier)
                                        .collect(Collectors.toList())))
                        .controller(StringControllerBuilder::create)
                        .initial("")
                        .build())
                .build())
        .build()
        .generateScreen(parent);
  }

  private static Component text(String key) {
    return Component.translatable(PerspectiveAPI.MOD_ID + "." + key);
  }
}
