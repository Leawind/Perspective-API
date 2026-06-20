package io.github.leawind.perspectiveapi.internal.impl;

import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record OverrideEntry(
    @NonNull Identifier key,
    int priority,
    @NonNull Supplier<@Nullable Identifier> supplier) {}
