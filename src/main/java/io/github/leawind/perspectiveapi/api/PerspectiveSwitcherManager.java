package io.github.leawind.perspectiveapi.api;

import java.util.List;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

@ApiStatus.Experimental
public interface PerspectiveSwitcherManager {
  @NonNull List<PerspectiveSwitcher> getSwitchers();

  @NonNull PerspectiveSwitcher getSwitcher();

  void setSwitcher(@NonNull PerspectiveSwitcher switcher);
}
