package com.mtxcore.unifiedlegacysettings;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

final class RuntimeCompatEnforcer {

  private static int ticks;

  private RuntimeCompatEnforcer() {}

  static void register() {
    ClientTickEvents.END_CLIENT_TICK.register(client -> {
      if (++ticks % 20 != 0)
        return;

      ZoomCompat.enforceRuntimeState();
      XaeroMinimapCompat.enforceRuntimeState();
      LocatorLodestonesCompat.enforceRuntimeState();
      ContinuityCompat.enforceRuntimeState();
    });
  }
}
