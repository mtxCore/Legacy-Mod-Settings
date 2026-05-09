package com.mtxcore.legacymodsettings;

public final class CompatDebug {

  private CompatDebug() {}

  public static boolean enabled() {
    return ModSettingsConfig.get().debugCompatLogs;
  }

  public static void setEnabled(boolean enabled) {
    ModSettingsConfig.mutateAndSave(cfg -> cfg.debugCompatLogs = enabled);
  }

  public static void log(String message, Object... args) {
    if (!enabled())
      return;
    LegacyModSettings.LOGGER.info("[LNS Debug] " + message, args);
  }
}
