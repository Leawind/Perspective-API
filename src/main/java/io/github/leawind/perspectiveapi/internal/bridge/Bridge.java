package io.github.leawind.perspectiveapi.internal.bridge;

import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;

public final class Bridge {
  private Bridge() {}

  /// Returns the current Minecraft data version number.
  public static int getDataVersion() {
    /*? if >=1.21.11 {*/
    return SharedConstants.getCurrentVersion().dataVersion().version();
    /*? } else {*/
    /*return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
     *//*? }*/
  }

  public static Identifier createIdentifier(String path) {
    return createIdentifier("minecraft", path);
  }

  public static Identifier createIdentifier(String namespace, String path) {
    /*? if >=1.21 {*/
    return Identifier.fromNamespaceAndPath(namespace, path);
    /*? } else {*/
    /*return new Identifier(namespace, path);
     *//*? }*/
  }
}
