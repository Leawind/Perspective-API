package io.github.leawind.perspectiveapi.internal.impl;

import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideChain;
import io.github.leawind.perspectiveapi.api.PerspectiveOverrideRegistration;
import io.github.leawind.perspectiveapi.api.PerspectiveRegistry;
import io.github.leawind.perspectiveapi.internal.utils.ExtensionInvoker;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerspectiveOverrideChainImpl
    implements PerspectiveOverrideChain, Supplier<@Nullable String> {
  private static final Logger LOGGER = LoggerFactory.getLogger(PerspectiveOverrideChainImpl.class);
  private static final ExtensionInvoker EXTENSIONS = new ExtensionInvoker(LOGGER, "Override entry");

  private final class Registration implements PerspectiveOverrideRegistration {
    private final String id;
    private final int priority;
    private final Supplier<@Nullable String> supplier;

    private Registration(
        @NonNull String id, int priority, @NonNull Supplier<@Nullable String> supplier) {
      this.id = id;
      this.priority = priority;
      this.supplier = supplier;
    }

    @Override
    public boolean unregister() {
      return PerspectiveOverrideChainImpl.this.unregister(this);
    }
  }

  private volatile List<Registration> entries = List.of();
  private final PerspectiveRegistry registry;

  public PerspectiveOverrideChainImpl(@NonNull PerspectiveRegistry registry) {
    this.registry = Objects.requireNonNull(registry);
  }

  @Override
  public @Nullable String get() {
    List<Registration> snapshot = this.entries;
    for (Registration entry : snapshot) {
      String resolvedId =
          EXTENSIONS.callOrElse(
              entry.id,
              "resolve",
              () -> {
                String candidate = entry.supplier.get();
                if (candidate == null) return null;
                Perspective perspective = registry.get(candidate);
                return perspective != null && perspective.isAvailable() ? candidate : null;
              },
              null);
      if (resolvedId != null) return resolvedId;
    }
    return null;
  }

  @Override
  public @NonNull PerspectiveOverrideRegistration register(
      @NonNull String id, int priority, @NonNull Supplier<@Nullable String> supplier) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(supplier);
    if (id.isEmpty()) throw new IllegalArgumentException("Override id must not be empty");
    Registration registration = new Registration(id, priority, supplier);
    synchronized (this) {
      if (entries.stream().anyMatch(entry -> entry.id.equals(id))) {
        throw new IllegalArgumentException("Override id is already registered: '" + id + "'");
      }
      List<Registration> newList = new ArrayList<>(entries);
      newList.add(registration);
      newList.sort(Comparator.comparingInt((Registration entry) -> entry.priority).reversed());
      this.entries = List.copyOf(newList);
    }
    return registration;
  }

  private boolean unregister(@NonNull Registration registration) {
    synchronized (this) {
      List<Registration> newList = new ArrayList<>(entries);
      if (newList.removeIf(entry -> entry == registration)) {
        this.entries = List.copyOf(newList);
        return true;
      }
      return false;
    }
  }
}
