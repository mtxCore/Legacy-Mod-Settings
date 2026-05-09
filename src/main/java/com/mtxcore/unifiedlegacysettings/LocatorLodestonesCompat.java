package com.mtxcore.unifiedlegacysettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public final class LocatorLodestonesCompat {

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().create();

  private static final String[] SETTING_FIELDS = {
      "TAB_SHOWS_NAMES",
      "SHOW_RECOVERY_COMPASSES",
      "SHOW_BUNDLED_COMPASSES",
  };
  private static final String TAB_DISPLAY_FIELD = "TAB_DISPLAY";
  private static Boolean locatorDesired;

  private LocatorLodestonesCompat() {}

  private static Boolean desiredLocatorState() {
    if (locatorDesired != null)
      return locatorDesired;
    return ModSettingsConfig.get().locatorCompassEnabled;
  }

  public static boolean isRuntimeEnabled() {
    Boolean desired = desiredLocatorState();
    return desired == null || desired;
  }

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!ModSettingsCompat.matchesSectionWithLegacyMerge(
            section, ModSettingsCompat.Section.ADVANCED_GAME_OPTIONS))
      return;
    if (!ModSettingsConfig.get().showLocatorLodestones)
      return;
    if (!RefUtil.isModLoaded("locator_lodestones"))
      return;

    Object[] settings = resolveSettings();
    if (settings.length == 0)
      return;

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "maps with coordinates",
        ()
            -> Component.literal("Locator Compass"),
        ()
            -> {
          boolean next = !getLocatorEnabled(settings);
          locatorDesired = next;
          RefUtil.persistModDesired(locatorDesired,
                                    RefUtil.PersistKey.LOCATOR_LODESTONES);
          CompatDebug.log("Locator Compass toggle -> {}", next);
          setTabDisplayEnabled(next);
          for (Object setting : settings) {
            setSettingEnabled(setting, next);
          }
          refreshWaypoints(next);
          updateLocatorConfigFile(next);
        },
        ()
            -> getLocatorEnabled(settings),
        ()
            -> Component.literal("Enable or disable all locator compass HUD "
                                 + "waypoints at once.")));
  }

  static void enforceRuntimeState() {
    Boolean desired = desiredLocatorState();
    if (desired == null)
      return;
    locatorDesired = desired;
    if (!RefUtil.isModLoaded("locator_lodestones"))
      return;

    Object[] settings = resolveSettings();
    boolean tabEnabled = isTabDisplayEnabled();
    boolean booleansEnabled = areAllEnabled(settings);
    boolean needsModeUpdate = tabEnabled != locatorDesired;
    boolean needsBooleanUpdate = booleansEnabled != locatorDesired;
    if (!needsModeUpdate && !needsBooleanUpdate)
      return;

    CompatDebug.log(
        "Enforcing Locator Compass -> {} (tabEnabled={}, booleansEnabled={})",
        locatorDesired, tabEnabled, booleansEnabled);

    if (needsModeUpdate) {
      setTabDisplayEnabled(locatorDesired);
    }
    if (needsBooleanUpdate) {
      for (Object setting : settings) {
        setSettingEnabled(setting, locatorDesired);
      }
    }
    refreshWaypoints(locatorDesired);
    updateLocatorConfigFile(locatorDesired);
  }

  private static Object[] resolveSettings() {
    return java.util.Arrays.stream(SETTING_FIELDS)
        .map(field
             -> RefUtil.staticField(
                 "net.pneumono.locator_lodestones.config.ConfigManager", field))
        .filter(Objects::nonNull)
        .toArray();
  }

  private static boolean areAllEnabled(Object[] settings) {
    if (settings.length == 0)
      return false;
    for (Object setting : settings) {
      if (!readSettingEnabled(setting)) {
        return false;
      }
    }
    return true;
  }

  private static boolean getLocatorEnabled(Object[] settings) {
    Boolean desired = desiredLocatorState();
    if (desired != null)
      return desired;
    return isTabDisplayEnabled() && areAllEnabled(settings);
  }

  private static boolean isTabDisplayEnabled() {
    Object value = readTabDisplayValue();
    return value != null && "DEFAULT".equals(value.toString());
  }

  private static Object readTabDisplayValue() {
    RefUtil.MethodRef tabDisplaySetting = RefUtil.staticMethod(
        "net.pneumono.locator_lodestones.config.ConfigManager",
        "tabDisplaySetting");
    Object directValue =
        tabDisplaySetting == null ? null : tabDisplaySetting.invokeStatic();
    if (directValue != null)
      return directValue;

    Object tabDisplay = RefUtil.staticField(
        "net.pneumono.locator_lodestones.config.ConfigManager",
        TAB_DISPLAY_FIELD);
    if (tabDisplay == null)
      return null;

    RefUtil.MethodRef getValue =
        RefUtil.method(tabDisplay.getClass(), "getValue");
    return getValue == null ? null : getValue.invoke(tabDisplay);
  }

  private static void setTabDisplayEnabled(boolean enabled) {
    Object tabDisplay = RefUtil.staticField(
        "net.pneumono.locator_lodestones.config.ConfigManager",
        TAB_DISPLAY_FIELD);
    if (tabDisplay == null)
      return;

    Class<?> displaySetting = RefUtil.classForName(
        "net.pneumono.locator_lodestones.config.DisplaySetting");
    if (displaySetting == null)
      return;

    Object mode =
        RefUtil.enumConstant(displaySetting, enabled ? "DEFAULT" : "TAB_ONLY");
    if (mode == null)
      return;

    if (trySetInstant(tabDisplay, mode))
      return;

    RefUtil.MethodRef setValue =
        RefUtil.method(tabDisplay.getClass(), "setValue", Object.class);
    if (setValue == null) {
      setValue =
          RefUtil.method(tabDisplay.getClass(), "setValue", mode.getClass());
    }
    if (setValue != null) {
      setValue.invoke(tabDisplay, mode);
      return;
    }

    RefUtil.MethodRef set =
        RefUtil.method(tabDisplay.getClass(), "set", Object.class);
    if (set == null) {
      set = RefUtil.method(tabDisplay.getClass(), "set", mode.getClass());
    }
    if (set != null) {
      set.invoke(tabDisplay, mode);
    }

    Object current = readTabDisplayValue();
    CompatDebug.log("Locator TAB_DISPLAY set request -> {} (current={})", mode,
                    current);
  }

  private static boolean readSettingEnabled(Object setting) {
    RefUtil.MethodRef getValue = RefUtil.method(setting.getClass(), "getValue");
    if (getValue != null) {
      return RefUtil.invokeBoolean(getValue, setting, true);
    }

    RefUtil.MethodRef get = RefUtil.method(setting.getClass(), "get");
    if (get != null) {
      return RefUtil.invokeBoolean(get, setting, true);
    }

    java.lang.reflect.Field value = RefUtil.field(setting.getClass(), "value");
    if (value != null) {
      return RefUtil.readBooleanField(setting, value, true);
    }
    return true;
  }

  private static void setSettingEnabled(Object setting, boolean enabled) {
    if (setting == null)
      return;

    if (trySetInstant(setting, enabled))
      return;

    RefUtil.MethodRef setValue =
        RefUtil.method(setting.getClass(), "setValue", boolean.class);
    if (setValue == null)
      setValue = RefUtil.method(setting.getClass(), "setValue", Boolean.class);
    if (setValue != null) {
      setValue.invoke(setting, enabled);
      return;
    }

    RefUtil.MethodRef set =
        RefUtil.method(setting.getClass(), "set", boolean.class);
    if (set == null)
      set = RefUtil.method(setting.getClass(), "set", Boolean.class);
    if (set != null) {
      set.invoke(setting, enabled);
      return;
    }

    RefUtil.MethodRef enableMethod =
        RefUtil.method(setting.getClass(), enabled ? "enable" : "disable");
    if (enableMethod != null) {
      enableMethod.invoke(setting);
      return;
    }

    java.lang.reflect.Field value = RefUtil.field(setting.getClass(), "value");
    if (value != null) {
      RefUtil.writeField(setting, value, enabled);
    }
  }

  private static boolean trySetInstant(Object setting, boolean enabled) {
    return trySetInstant(setting, Boolean.valueOf(enabled));
  }

  private static boolean trySetInstant(Object setting, Object value) {
    Class<?> abstractConfigClass =
        RefUtil.classForName("net.pneumono.pneumonocore.config_api."
                             + "configurations.AbstractConfiguration");
    Class<?> loadTypeClass = RefUtil.classForName(
        "net.pneumono.pneumonocore.config_api.enums.LoadType");
    Class<?> serverClass =
        RefUtil.classForName("net.minecraft.server.MinecraftServer");

    RefUtil.MethodRef setInstantValue =
        (abstractConfigClass != null && loadTypeClass != null &&
         serverClass != null)
            ? RefUtil.staticMethod("net.pneumono.pneumonocore.config_api."
                                       + "configurations.ConfigManager",
                                   "setValue", abstractConfigClass,
                                   Object.class, loadTypeClass, serverClass)
            : null;
    Object loadTypeInstant =
        loadTypeClass != null ? RefUtil.enumConstant(loadTypeClass, "INSTANT")
                              : null;

    if (setInstantValue != null && loadTypeInstant != null) {
      setInstantValue.invokeStatic(setting, value, loadTypeInstant, null);
      return true;
    }

    return false;
  }

  private static void refreshWaypoints(boolean enabled) {
    // markWaypointsDirty preserves the previous waypoint set long enough for
    // untracking to run cleanly.
    RefUtil.invoke(
        RefUtil.staticMethod("net.pneumono.locator_lodestones.WaypointTracking",
                             "markWaypointsDirty"),
        null);
  }

  private static void updateLocatorConfigFile(boolean enabled) {
    Path file = FabricLoader.getInstance().getGameDir().resolve(
        "config/locator_lodestones.json");
    if (!Files.exists(file))
      return;

    try {
      JsonElement root = JsonParser.parseString(Files.readString(file));
      if (!(root instanceof JsonObject obj))
        return;

      obj.addProperty("tab_shows_names", enabled);
      obj.addProperty("show_recovery_compasses", enabled);
      obj.addProperty("show_bundled_compasses", enabled);
      obj.addProperty("tab_display", enabled ? "DEFAULT" : "TAB_ONLY");

      Files.writeString(file, GSON.toJson(obj));
    } catch (IOException e) {
      CompatDebug.log("Failed to update locator_lodestones.json", e);
    }
  }
}
