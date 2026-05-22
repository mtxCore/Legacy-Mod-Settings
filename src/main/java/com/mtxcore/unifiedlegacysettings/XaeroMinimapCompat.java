package com.mtxcore.unifiedlegacysettings;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.network.chat.Component;

final class XaeroMinimapCompat {

  private static Boolean minimapDesired;

  private XaeroMinimapCompat() {}

  private static Boolean desiredState() {
    return minimapDesired != null ? minimapDesired
                                  : ModSettingsConfig.get().xaeroMinimapEnabled;
  }

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!ModSettingsCompat.matchesSectionWithLegacyMerge(
            section, ModSettingsCompat.Section.GAME_OPTIONS))
      return;
    if (!ModSettingsConfig.get().showXaeroMinimap)
      return;
    if (!RefUtil.isModLoaded("xaerominimap", "xaeros_minimap", "xaerosminimap"))
      return;

    XaeroApi api = resolveApi();
    if (api != null) {
      entries.add(ModSettingsCompat.Entry.toggleBefore(
          "after:dynamic lighting|view bobbing",
          ()
              -> Component.literal("Minimap"),
          ()
              -> {
            boolean next = !readEnabled(api);
            minimapDesired = next;
            RefUtil.persistModDesired(next, RefUtil.PersistKey.XAERO_MINIMAP);
            applyEnabled(api, next);
          },
          ()
              -> readEnabled(api),
          ()
              -> Component.literal("Show a minimap of your surroundings in " +
                                   "the corner of the screen.")));
      return;
    }

    XaeroLegacyApi legacy = resolveLegacyApi();
    if (legacy == null)
      return;

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "after:dynamic lighting|view bobbing",
        ()
            -> Component.literal("Minimap"),
        ()
            -> {
          boolean next = !readEnabledLegacy(legacy);
          minimapDesired = next;
          RefUtil.persistModDesired(next, RefUtil.PersistKey.XAERO_MINIMAP);
          applyEnabledLegacy(legacy, next);
        },
        ()
            -> readEnabledLegacy(legacy),
        ()
            -> Component.literal("Show a minimap of your surroundings in the " +
                                 "corner of the screen.")));
  }

  static void enforceRuntimeState() {
    Boolean desired = desiredState();
    if (desired == null)
      return;
    minimapDesired = desired;
    if (!RefUtil.isModLoaded("xaerominimap", "xaeros_minimap", "xaerosminimap"))
      return;

    XaeroApi api = resolveApi();
    if (api != null) {
      if (readEnabled(api) != desired)
        applyEnabled(api, desired);
      return;
    }
    XaeroLegacyApi legacy = resolveLegacyApi();
    if (legacy != null && readEnabledLegacy(legacy) != desired) {
      applyEnabledLegacy(legacy, desired);
    }
  }

  private static XaeroApi resolveApi() {
    Object hudMod = RefUtil.staticField("xaero.common.HudMod", "INSTANCE");
    if (hudMod == null)
      return null;

    RefUtil.MethodRef getSettings =
        RefUtil.method(hudMod.getClass(), "getSettings");
    Object settings = getSettings == null ? null : getSettings.invoke(hudMod);
    if (settings == null)
      return null;

    RefUtil.MethodRef saveSettings =
        RefUtil.method(settings.getClass(), "saveSettings");

    Object channel = RefUtil.invoke(
        RefUtil.method(hudMod.getClass(), "getHudConfigs"), hudMod);
    Object clientMgr = RefUtil.invoke(
        RefUtil.method(channel == null ? null : channel.getClass(),
                       "getClientConfigManager"),
        channel);
    Object primaryMgr = RefUtil.invoke(
        RefUtil.method(clientMgr == null ? null : clientMgr.getClass(),
                       "getPrimaryConfigManager"),
        clientMgr);
    Object config = RefUtil.invoke(
        RefUtil.method(primaryMgr == null ? null : primaryMgr.getClass(),
                       "getConfig"),
        primaryMgr);

    Object displayMinimapOption = RefUtil.staticField(
        "xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions",
        "DISPLAY_MINIMAP");
    Class<?> configOptionClass =
        RefUtil.classForName("xaero.lib.common.config.option.ConfigOption");

    if (config == null || displayMinimapOption == null ||
        configOptionClass == null)
      return null;

    RefUtil.MethodRef setOption = RefUtil.method(
        config.getClass(), "set", configOptionClass, Object.class);
    RefUtil.MethodRef getOption =
        RefUtil.method(config.getClass(), "get", configOptionClass);
    if (setOption == null)
      return null;

    return new XaeroApi(settings, saveSettings, config,
                        displayMinimapOption, setOption, getOption);
  }

  private static boolean readEnabled(XaeroApi api) {
    Boolean desired = desiredState();
    if (desired != null)
      return desired;

    Object value =
        api.getOption() == null
            ? null
            : api.getOption().invoke(api.config(), api.displayMinimapOption());
    if (value instanceof Boolean b)
      return b;
    return true;
  }

  private static void applyEnabled(XaeroApi api, boolean enabled) {
    syncHudModReadSetting(api.settings(), enabled);
    api.setOption().invoke(api.config(), api.displayMinimapOption(), enabled);
    RefUtil.invoke(api.saveSettings(), api.settings());
  }

  private static void syncHudModReadSetting(Object settings, boolean enabled) {
    RefUtil.MethodRef readSetting =
        RefUtil.method(settings.getClass(), "readSetting", String[].class);
    if (readSetting != null)
      readSetting.invoke(settings,
                         (Object) new String[] {"minimap",
                                                Boolean.toString(enabled)});
    RefUtil.invoke(RefUtil.method(settings.getClass(), "saveSettings"),
                   settings);
  }

  private record
      XaeroApi(Object settings, RefUtil.MethodRef saveSettings, Object config,
               Object displayMinimapOption, RefUtil.MethodRef setOption,
               RefUtil.MethodRef getOption) {}

  private record XaeroLegacyApi(Object settingsMgr, Object cfg,
                                Field minimapEnabled,
                                RefUtil.MethodRef saveSettings) {}

  private static XaeroLegacyApi resolveLegacyApi() {
    Object mgr =
        RefUtil.staticField("xaero.minimap.XaeroMinimap", "SETTINGS_MGR");
    if (mgr == null)
      return null;

    RefUtil.MethodRef getCfg = RefUtil.method(mgr.getClass(), "getSettings");
    if (getCfg == null)
      return null;

    Object cfg = getCfg.invoke(mgr);
    if (cfg == null)
      return null;

    Field enabled = RefUtil.field(cfg.getClass(), "minimapEnabled");
    if (enabled == null)
      return null;

    RefUtil.MethodRef save = RefUtil.method(mgr.getClass(), "saveSettings");

    return new XaeroLegacyApi(mgr, cfg, enabled, save);
  }

  private static boolean readEnabledLegacy(XaeroLegacyApi api) {
    Boolean desired = desiredState();
    if (desired != null)
      return desired;
    return RefUtil.readBooleanField(api.cfg(), api.minimapEnabled(), true);
  }

  private static void applyEnabledLegacy(XaeroLegacyApi api, boolean enabled) {
    RefUtil.writeField(api.cfg(), api.minimapEnabled(), enabled);
    RefUtil.invoke(api.saveSettings(), api.settingsMgr());
  }
}
