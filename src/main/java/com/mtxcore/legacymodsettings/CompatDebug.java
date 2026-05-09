package com.mtxcore.legacymodsettings;

final class CompatDebug {

  private CompatDebug() {}

  static boolean enabled() { return ModSettingsConfig.get().debugCompatLogs; }

  static void setEnabled(boolean enabled) {
    ModSettingsConfig.mutateAndSave(cfg -> cfg.debugCompatLogs = enabled);
  }

  static void log(String message, Object... args) {
    if (!enabled())
      return;
    LegacyModSettings.LOGGER.info("[LNS Debug] " + message, args);
  }
}
