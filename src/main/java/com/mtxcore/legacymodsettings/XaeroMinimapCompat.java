package com.mtxcore.legacymodsettings;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.network.chat.Component;

final class XaeroMinimapCompat {

  private static Boolean minimapDesired;

  private XaeroMinimapCompat() {}

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!ModSettingsCompat.matchesSectionWithLegacyMerge(
            section, ModSettingsCompat.Section.GAME_OPTIONS))
      return;
    if (!ModSettingsConfig.get().showXaeroMinimap)
      return;
    if (!RefUtil.isModLoaded("xaerominimap", "xaeros_minimap", "xaerosminimap"))
      return;

    Object hudMod = RefUtil.staticField("xaero.common.HudMod", "INSTANCE");
    if (hudMod != null) {
      RefUtil.MethodRef getSettingsFromHud =
          RefUtil.method(hudMod.getClass(), "getSettings");
      Object settingsObj =
          getSettingsFromHud == null ? null : getSettingsFromHud.invoke(hudMod);
      RefUtil.MethodRef getMinimap =
          settingsObj == null
              ? null
              : RefUtil.method(settingsObj.getClass(), "getMinimap");
      RefUtil.MethodRef saveSettings =
          settingsObj == null
              ? null
              : RefUtil.method(settingsObj.getClass(), "saveSettings");

      RefUtil.MethodRef getHudConfigs =
          RefUtil.method(hudMod.getClass(), "getHudConfigs");
      Object channel =
          getHudConfigs == null ? null : getHudConfigs.invoke(hudMod);
      RefUtil.MethodRef getClientConfigManager =
          channel == null
              ? null
              : RefUtil.method(channel.getClass(), "getClientConfigManager");
      Object clientCfgMgr = getClientConfigManager == null
                                ? null
                                : getClientConfigManager.invoke(channel);
      RefUtil.MethodRef getPrimaryConfigManager =
          clientCfgMgr == null ? null
                               : RefUtil.method(clientCfgMgr.getClass(),
                                                "getPrimaryConfigManager");
      Object primaryCfgMgr = getPrimaryConfigManager == null
                                 ? null
                                 : getPrimaryConfigManager.invoke(clientCfgMgr);
      RefUtil.MethodRef getConfig =
          primaryCfgMgr == null
              ? null
              : RefUtil.method(primaryCfgMgr.getClass(), "getConfig");
      Object config =
          getConfig == null ? null : getConfig.invoke(primaryCfgMgr);
      Object displayMinimapOption = RefUtil.staticField(
          "xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions",
          "DISPLAY_MINIMAP");
      Class<?> configOptionClass =
          RefUtil.classForName("xaero.lib.common.config.option.ConfigOption");
      RefUtil.MethodRef setOption =
          (config == null || configOptionClass == null)
              ? null
              : RefUtil.method(config.getClass(), "set", configOptionClass,
                               Object.class);

      RefUtil.MethodRef getOption =
          (config == null || configOptionClass == null)
              ? null
              : RefUtil.method(config.getClass(), "get", configOptionClass);

      if (settingsObj != null && getMinimap != null && config != null &&
          displayMinimapOption != null && setOption != null) {
        final Object finalSettings = settingsObj;
        final RefUtil.MethodRef finalGetMinimap = getMinimap;
        final Object finalConfig = config;
        final Object finalDisplayMinimapOption = displayMinimapOption;
        final RefUtil.MethodRef finalSetOption = setOption;
        final RefUtil.MethodRef finalGetOption = getOption;
        final RefUtil.MethodRef finalSaveSettings = saveSettings;

        entries.add(ModSettingsCompat.Entry.toggleBefore(
            "after:dynamic lighting|view bobbing",
            ()
                -> Component.literal("Minimap"),
            ()
                -> {
              boolean next = !getMinimapEnabled(finalGetOption, finalConfig,
                                                finalDisplayMinimapOption,
                                                finalGetMinimap, finalSettings);
              minimapDesired = next;
              CompatDebug.log("Minimap toggle (modern path) -> {}", next);
              applyModSettingsToggle(next);
              finalSetOption.invoke(finalConfig, finalDisplayMinimapOption,
                                    next);
              applyBooleanSetter(finalSettings, next, "setMinimap",
                                 "setDisplayMinimap", "setEnabled");
              if (finalSaveSettings != null)
                finalSaveSettings.invoke(finalSettings);
            },
            ()
                -> getMinimapEnabled(finalGetOption, finalConfig,
                                     finalDisplayMinimapOption, finalGetMinimap,
                                     finalSettings),
            ()
                -> Component.literal("Show a minimap of your surroundings in "
                                     + "the corner of the screen.")));
        return;
      }
    }

    Object settings =
        RefUtil.staticField("xaero.minimap.XaeroMinimap", "SETTINGS_MGR");
    if (settings == null)
      settings = RefUtil.staticField("xaero.minimap.XaeroMinimap", "SETTINGS");
    if (settings == null)
      settings = RefUtil.staticField("xaero.minimap.XaeroMinimap", "settings");
    if (settings == null)
      return;

    RefUtil.MethodRef getSettings =
        RefUtil.method(settings.getClass(), "getSettings");
    if (getSettings == null)
      getSettings = RefUtil.method(settings.getClass(), "getMainConfig");
    if (getSettings == null)
      return;
    Object cfg = getSettings.invoke(settings);
    if (cfg == null)
      return;

    Field enabled = RefUtil.field(cfg.getClass(), "minimapEnabled");
    if (enabled == null)
      enabled = RefUtil.field(cfg.getClass(), "enabled");
    if (enabled == null)
      enabled = RefUtil.field(cfg.getClass(), "displayMinimap");
    if (enabled == null)
      return;

    RefUtil.MethodRef save =
        RefUtil.method(settings.getClass(), "saveSettings");
    if (save == null)
      save = RefUtil.method(settings.getClass(), "save");

    final Object finalCfg = cfg;
    final Field finalEnabled = enabled;
    final RefUtil.MethodRef finalSave = save;
    final Object finalSettings = settings;

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "after:dynamic lighting|view bobbing",
        ()
            -> Component.literal("Minimap"),
        ()
            -> {
          boolean next = !getMinimapEnabled(null, null, null, null, null,
                                            finalCfg, finalEnabled);
          minimapDesired = next;
          CompatDebug.log("Minimap toggle (legacy path) -> {}", next);
          RefUtil.writeField(finalCfg, finalEnabled, next);
          applyBooleanSetter(finalCfg, next, "setMinimap", "setDisplayMinimap",
                             "setEnabled");
          applyBooleanSetter(finalSettings, next, "setMinimap",
                             "setDisplayMinimap", "setEnabled");
          RefUtil.invoke(finalSave, finalSettings);
        },
        ()
            -> getMinimapEnabled(null, null, null, null, null, finalCfg,
                                 finalEnabled),
        ()
            -> Component.literal("Show a minimap of your surroundings in the "
                                 + "corner of the screen.")));
  }

  static void enforceRuntimeState() {
    if (minimapDesired == null)
      return;
    if (!RefUtil.isModLoaded("xaerominimap", "xaeros_minimap", "xaerosminimap"))
      return;

    Object hudMod = RefUtil.staticField("xaero.common.HudMod", "INSTANCE");
    if (hudMod != null) {
      RefUtil.MethodRef getSettingsFromHud =
          RefUtil.method(hudMod.getClass(), "getSettings");
      Object settingsObj =
          getSettingsFromHud == null ? null : getSettingsFromHud.invoke(hudMod);
      RefUtil.MethodRef saveSettings =
          settingsObj == null
              ? null
              : RefUtil.method(settingsObj.getClass(), "saveSettings");

      RefUtil.MethodRef getHudConfigs =
          RefUtil.method(hudMod.getClass(), "getHudConfigs");
      Object channel =
          getHudConfigs == null ? null : getHudConfigs.invoke(hudMod);
      RefUtil.MethodRef getClientConfigManager =
          channel == null
              ? null
              : RefUtil.method(channel.getClass(), "getClientConfigManager");
      Object clientCfgMgr = getClientConfigManager == null
                                ? null
                                : getClientConfigManager.invoke(channel);
      RefUtil.MethodRef getPrimaryConfigManager =
          clientCfgMgr == null ? null
                               : RefUtil.method(clientCfgMgr.getClass(),
                                                "getPrimaryConfigManager");
      Object primaryCfgMgr = getPrimaryConfigManager == null
                                 ? null
                                 : getPrimaryConfigManager.invoke(clientCfgMgr);
      RefUtil.MethodRef getConfig =
          primaryCfgMgr == null
              ? null
              : RefUtil.method(primaryCfgMgr.getClass(), "getConfig");
      Object config =
          getConfig == null ? null : getConfig.invoke(primaryCfgMgr);
      Object displayMinimapOption = RefUtil.staticField(
          "xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions",
          "DISPLAY_MINIMAP");
      Class<?> configOptionClass =
          RefUtil.classForName("xaero.lib.common.config.option.ConfigOption");
      RefUtil.MethodRef setOption =
          (config == null || configOptionClass == null)
              ? null
              : RefUtil.method(config.getClass(), "set", configOptionClass,
                               Object.class);
      RefUtil.MethodRef getOption =
          (config == null || configOptionClass == null)
              ? null
              : RefUtil.method(config.getClass(), "get", configOptionClass);
      if (setOption != null && displayMinimapOption != null) {
        boolean current = readMinimapEnabled(
            getOption, config, displayMinimapOption, null, settingsObj);
        if (current != minimapDesired) {
          CompatDebug.log("Enforcing minimap -> {}", minimapDesired);
          applyModSettingsToggle(minimapDesired);
          setOption.invoke(config, displayMinimapOption, minimapDesired);
        }
        applyBooleanSetter(settingsObj, minimapDesired, "setMinimap",
                           "setDisplayMinimap", "setEnabled");
        if (saveSettings != null)
          saveSettings.invoke(settingsObj);
      }
    }

    Object settings =
        RefUtil.staticField("xaero.minimap.XaeroMinimap", "SETTINGS_MGR");
    if (settings == null)
      settings = RefUtil.staticField("xaero.minimap.XaeroMinimap", "SETTINGS");
    if (settings == null)
      settings = RefUtil.staticField("xaero.minimap.XaeroMinimap", "settings");
    if (settings == null)
      return;

    RefUtil.MethodRef getSettings =
        RefUtil.method(settings.getClass(), "getSettings");
    if (getSettings == null)
      getSettings = RefUtil.method(settings.getClass(), "getMainConfig");
    Object cfg = getSettings == null ? null : getSettings.invoke(settings);
    if (cfg == null)
      return;

    Field enabled = RefUtil.field(cfg.getClass(), "minimapEnabled");
    if (enabled == null)
      enabled = RefUtil.field(cfg.getClass(), "enabled");
    if (enabled == null)
      enabled = RefUtil.field(cfg.getClass(), "displayMinimap");
    if (enabled != null) {
      boolean current = RefUtil.readBooleanField(cfg, enabled, true);
      if (current != minimapDesired) {
        CompatDebug.log("Enforcing minimap -> {}", minimapDesired);
        applyModSettingsToggle(minimapDesired);
        RefUtil.writeField(cfg, enabled, minimapDesired);
      }
    }
    applyBooleanSetter(cfg, minimapDesired, "setMinimap", "setDisplayMinimap",
                       "setEnabled");
    applyBooleanSetter(settings, minimapDesired, "setMinimap",
                       "setDisplayMinimap", "setEnabled");
    RefUtil.MethodRef save =
        RefUtil.method(settings.getClass(), "saveSettings");
    if (save == null)
      save = RefUtil.method(settings.getClass(), "save");
    RefUtil.invoke(save, settings);
  }

  private static void applyModSettingsToggle(boolean enabled) {
    Object hudMod = RefUtil.staticField("xaero.common.HudMod", "INSTANCE");
    if (hudMod == null)
      return;

    RefUtil.MethodRef getSettings =
        RefUtil.method(hudMod.getClass(), "getSettings");
    Object settings = getSettings == null ? null : getSettings.invoke(hudMod);
    if (settings == null)
      return;

    RefUtil.MethodRef readSetting =
        RefUtil.method(settings.getClass(), "readSetting", String[].class);
    if (readSetting != null) {
      readSetting.invoke(settings, (Object) new String[] {
                                       "minimap", Boolean.toString(enabled)});
    }

    RefUtil.MethodRef saveSettings =
        RefUtil.method(settings.getClass(), "saveSettings");
    RefUtil.invoke(saveSettings, settings);
  }

  private static boolean readMinimapEnabled(RefUtil.MethodRef getOption,
                                            Object config,
                                            Object displayMinimapOption,
                                            RefUtil.MethodRef getMinimap,
                                            Object settings) {
    RefUtil.MethodRef getModMinimap =
        settings == null ? null
                         : RefUtil.method(settings.getClass(), "getMinimap");
    if (getModMinimap != null) {
      return RefUtil.invokeBoolean(getModMinimap, settings, true);
    }

    Object value = getOption == null
                       ? null
                       : getOption.invoke(config, displayMinimapOption);
    if (value instanceof Boolean b)
      return b;
    return RefUtil.invokeBoolean(getMinimap, settings, true);
  }

  private static boolean getMinimapEnabled(RefUtil.MethodRef getOption,
                                           Object config,
                                           Object displayMinimapOption,
                                           RefUtil.MethodRef getMinimap,
                                           Object settings) {
    if (minimapDesired != null)
      return minimapDesired;
    return readMinimapEnabled(getOption, config, displayMinimapOption,
                              getMinimap, settings);
  }

  private static boolean
  getMinimapEnabled(RefUtil.MethodRef getOption, Object config,
                    Object displayMinimapOption, RefUtil.MethodRef getMinimap,
                    Object settings, Object cfg, Field enabledField) {
    if (minimapDesired != null)
      return minimapDesired;
    if (enabledField != null)
      return RefUtil.readBooleanField(cfg, enabledField, true);
    return readMinimapEnabled(getOption, config, displayMinimapOption,
                              getMinimap, settings);
  }

  private static void applyBooleanSetter(Object target, boolean value,
                                         String... names) {
    if (target == null || names == null)
      return;

    for (String name : names) {
      RefUtil.MethodRef m =
          RefUtil.method(target.getClass(), name, boolean.class);
      if (m == null)
        m = RefUtil.method(target.getClass(), name, Boolean.class);
      if (m != null) {
        m.invoke(target, value);
        return;
      }
    }
  }
}
