package com.mtxcore.unifiedlegacysettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

public final class LocatorLodestonesCompat {

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().create();

  // These fields are the mod's HUD-facing switches; the compass tracking mixin
  // reads the same desired state so we do not need to patch every waypoint type.
  private static final String[] BOOL_FIELDS = {
      "TAB_SHOWS_NAMES",
      "SHOW_RECOVERY_COMPASSES",
      "SHOW_BUNDLED_COMPASSES",
  };

  private static Boolean locatorDesired;

  private LocatorLodestonesCompat() {}

  private static Boolean desiredState() {
    return locatorDesired != null
        ? locatorDesired
        : ModSettingsConfig.get().locatorCompassEnabled;
  }

  public static boolean isRuntimeEnabled() {
    Boolean d = desiredState();
    return d == null || d;
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
          boolean next = !isEnabled(settings);
          locatorDesired = next;
          RefUtil.persistModDesired(next,
                                    RefUtil.PersistKey.LOCATOR_LODESTONES);
          applyAll(settings, next);
        },
        ()
            -> isEnabled(settings),
        ()
            -> Component.literal("Enable or disable all locator compass HUD "
                                 + "waypoints at once.")));
  }

  static void enforceRuntimeState() {
    Boolean desired = desiredState();
    if (desired == null)
      return;
    locatorDesired = desired;
    if (!RefUtil.isModLoaded("locator_lodestones"))
      return;

    Object[] settings = resolveSettings();
    boolean tabOk = isTabDisplayEnabled() == desired;
    boolean boolOk = areAllEnabled(settings) == desired;
    if (tabOk && boolOk)
      return;

    applyAll(settings, desired);
  }

  private static void applyAll(Object[] settings, boolean enabled) {
    setTabDisplayEnabled(enabled);
    for (Object s : settings)
      setSettingEnabled(s, enabled);
    markWaypointsDirty();
    writeConfigFile(enabled);
  }

  private static Object[] resolveSettings() {
    return Arrays.stream(BOOL_FIELDS)
        .map(f
             -> RefUtil.staticField(
                 "net.pneumono.locator_lodestones.config.ConfigManager", f))
        .filter(Objects::nonNull)
        .toArray();
  }

  private static boolean areAllEnabled(Object[] settings) {
    if (settings.length == 0)
      return false;
    for (Object s : settings)
      if (!readBoolean(s))
        return false;
    return true;
  }

  private static boolean isEnabled(Object[] settings) {
    Boolean desired = desiredState();
    if (desired != null)
      return desired;
    return isTabDisplayEnabled() && areAllEnabled(settings);
  }

  private static boolean isTabDisplayEnabled() {
    Object val = readTabDisplayValue();
    return val != null && "DEFAULT".equals(val.toString());
  }

  private static Object readTabDisplayValue() {
    Object tabDisplay = RefUtil.staticField(
        "net.pneumono.locator_lodestones.config.ConfigManager", "TAB_DISPLAY");
    if (tabDisplay == null)
      return null;
    RefUtil.MethodRef get = RefUtil.method(tabDisplay.getClass(), "getValue");
    if (get == null)
      get = RefUtil.method(tabDisplay.getClass(), "get");
    return get == null ? null : get.invoke(tabDisplay);
  }

  private static void setTabDisplayEnabled(boolean enabled) {
    Object tabDisplay = RefUtil.staticField(
        "net.pneumono.locator_lodestones.config.ConfigManager", "TAB_DISPLAY");
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

    setPneumonoConfig(tabDisplay, mode);
  }

  private static boolean readBoolean(Object setting) {
    RefUtil.MethodRef get = RefUtil.method(setting.getClass(), "getValue");
    return get == null || RefUtil.invokeBoolean(get, setting, true);
  }

  private static void setSettingEnabled(Object setting, boolean enabled) {
    if (setting == null)
      return;
    setPneumonoConfig(setting, enabled);
  }

  private static void setPneumonoConfig(Object setting, Object value) {
    Class<?> abstractCfg =
        RefUtil.classForName("net.pneumono.pneumonocore.config_api."
                             + "configurations.AbstractConfiguration");
    Class<?> loadTypeClass = RefUtil.classForName(
        "net.pneumono.pneumonocore.config_api.enums.LoadType");
    Class<?> serverClass =
        RefUtil.classForName("net.minecraft.server.MinecraftServer");
    if (abstractCfg == null || loadTypeClass == null || serverClass == null)
      return;

    RefUtil.MethodRef setInstant = RefUtil.staticMethod(
        "net.pneumono.pneumonocore.config_api.configurations.ConfigManager",
        "setValue", abstractCfg, Object.class, loadTypeClass, serverClass);
    Object instant = RefUtil.enumConstant(loadTypeClass, "INSTANT");
    if (setInstant == null || instant == null)
      return;

    setInstant.invokeStatic(setting, value, instant, null);
  }

  private static void markWaypointsDirty() {
    RefUtil.invoke(
        RefUtil.staticMethod("net.pneumono.locator_lodestones.WaypointTracking",
                             "markWaypointsDirty"),
        null);
  }

  // Write directly to the JSON config so changes survive a restart even if
  // the in-memory objects don't flush themselves.
  private static void writeConfigFile(boolean enabled) {
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
    } catch (IOException ignored) {
      // The live config has already been updated; losing the disk write just
      // means Locator Lodestones will restore its own defaults next launch.
    }
  }
}
