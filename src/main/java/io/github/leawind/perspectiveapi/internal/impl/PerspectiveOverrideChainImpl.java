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
    private final int priority;
    private final Supplier<@Nullable String> supplier;

    private Registration(int priority, @NonNull Supplier<@Nullable String> supplier) {
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
      String diagnosticId = entry.supplier.getClass().getName();
      String resolvedId =
          EXTENSIONS.callOrElse(
              diagnosticId,
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
      int priority, @NonNull Supplier<@Nullable String> supplier) {
    Objects.requireNonNull(supplier);
    Registration registration = new Registration(priority, supplier);
    synchronized (this) {
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

  /// Clears all entries except the registration identified by the given handle.
  public void clearExcept(@NonNull PerspectiveOverrideRegistration registration) {
    Objects.requireNonNull(registration);
    synchronized (this) {
      List<Registration> newList = new ArrayList<>(entries);
      if (newList.removeIf(entry -> entry != registration)) {
        this.entries = List.copyOf(newList);
      }
    }
  }
}
