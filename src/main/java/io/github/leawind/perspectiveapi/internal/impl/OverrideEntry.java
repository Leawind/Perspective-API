package io.github.leawind.perspectiveapi.internal.impl;

import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Represents an entry in the perspective override chain.
///
/// @param key unique identifier for this override entry
/// @param priority evaluation priority. higher values are evaluated first
/// @param supplier provides the perspective id to use, or null to skip this entry
public record OverrideEntry(
    @NonNull Identifier key, int priority, @NonNull Supplier<@Nullable Identifier> supplier) {}
