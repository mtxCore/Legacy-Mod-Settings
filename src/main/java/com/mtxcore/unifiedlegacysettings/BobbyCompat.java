package com.mtxcore.unifiedlegacysettings;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.network.chat.Component;

final class BobbyCompat {

  private BobbyCompat() {}

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (section != ModSettingsCompat.Section.ADVANCED_GRAPHICS)
      return;
    if (!ModSettingsConfig.get().showBobbyOptions)
      return;
    if (!RefUtil.isModLoaded("bobby"))
      return;

    Object cfg = resolveConfig();
    if (cfg == null) {
      CompatDebug.log("BobbyCompat: could not resolve Bobby config");
      return;
    }

    RefUtil.MethodRef isEnabledMethod =
        RefUtil.method(cfg.getClass(), "isEnabled");
    Field enabled = RefUtil.field(cfg.getClass(), "enabled");
    if (enabled == null)
      enabled = RefUtil.field(cfg.getClass(), "enableBobby");

    if (isEnabledMethod == null && enabled == null) {
      CompatDebug.log("BobbyCompat: no enabled accessor on {}",
                      cfg.getClass().getName());
      return;
    }

    RefUtil.MethodRef save = RefUtil.method(cfg.getClass(), "save");
    if (save == null)
      save = RefUtil.staticMethod("de.johni0702.bobby.BobbyConfig", "save");

    final Object finalCfg = cfg;
    final RefUtil.MethodRef finalIsEnabledMethod = isEnabledMethod;
    final Field finalEnabled = enabled;
    final RefUtil.MethodRef finalSave = save;

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "after:entity shadows|legacy settings|override terrain fog "
            + "start|terrain fog start",
        ()
            -> Component.literal("Extended Online Render Distance"),
        ()
            -> {
          boolean next =
              !isEnabled(finalCfg, finalIsEnabledMethod, finalEnabled);
          if (finalEnabled != null)
            RefUtil.writeField(finalCfg, finalEnabled, next);
          RefUtil.invoke(finalSave, finalCfg);
        },
        ()
            -> isEnabled(finalCfg, finalIsEnabledMethod, finalEnabled),
        ()
            -> Component.literal(
                "Use cached chunks to see further than the server allows.")));
  }

  private static Object resolveConfig() {
    RefUtil.MethodRef getInstance = RefUtil.staticMethod(
        "de.johni0702.minecraft.bobby.Bobby", "getInstance");
    if (getInstance != null) {
      Object bobby = getInstance.invokeStatic();
      if (bobby != null) {
        RefUtil.MethodRef getConfig =
            RefUtil.method(bobby.getClass(), "getConfig");
        if (getConfig != null) {
          Object cfg = getConfig.invoke(bobby);
          if (cfg != null)
            return cfg;
        }
      }
    }
    return RefUtil.staticField("de.johni0702.bobby.BobbyConfig", "INSTANCE");
  }

  private static boolean
  isEnabled(Object cfg, RefUtil.MethodRef isEnabledMethod, Field enabledField) {
    if (isEnabledMethod != null)
      return RefUtil.invokeBoolean(isEnabledMethod, cfg, true);
    return RefUtil.readBooleanField(cfg, enabledField, true);
  }
}
