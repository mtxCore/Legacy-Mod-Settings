package com.mtxcore.unifiedlegacysettings;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class ZoomCompat {

  private static InputConstants.Key savedZoomKey;
  private static InputConstants.Key savedSecondaryKey;
  private static Boolean zoomDesired;

  private ZoomCompat() {}

  private static Boolean desiredState() {
    return zoomDesired != null ? zoomDesired
                               : ModSettingsConfig.get().zoomEnabled;
  }

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!ModSettingsCompat.matchesSectionWithLegacyMerge(
            section, ModSettingsCompat.Section.ADVANCED_GAME_OPTIONS))
      return;
    if (!ModSettingsConfig.get().showZoom)
      return;
    if (!RefUtil.isModLoaded("zoomify"))
      return;

    KeyMapping primary = findZoomKey("zoomify.key.zoom", "zoomKey");
    KeyMapping secondary =
        findZoomKey("zoomify.key.zoom.secondary", "secondaryZoomKey");

    Object cfg = zoomifySettings();

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "maps with coordinates",
        ()
            -> Component.literal("Zoom"),
        ()
            -> {
          boolean next = !getEnabled(cfg, primary, secondary);
          zoomDesired = next;
          RefUtil.persistModDesired(next, RefUtil.PersistKey.ZOOM);
          applyState(cfg, primary, secondary, next);
        },
        ()
            -> getEnabled(cfg, primary, secondary),
        ()
            -> Component.literal(
                "Magnify your view while holding the zoom control.")));
  }

  static void enforceRuntimeState() {
    Boolean desired = desiredState();
    if (desired == null)
      return;
    zoomDesired = desired;
    if (!RefUtil.isModLoaded("zoomify"))
      return;

    KeyMapping primary = findZoomKey("zoomify.key.zoom", "zoomKey");
    KeyMapping secondary =
        findZoomKey("zoomify.key.zoom.secondary", "secondaryZoomKey");
    Object cfg = zoomifySettings();

    if (getEnabled(cfg, primary, secondary) != desired)
      applyState(cfg, primary, secondary, desired);

    if (!desired)
      killActiveZoom();
  }

  private static boolean getEnabled(Object cfg, KeyMapping primary,
                                    KeyMapping secondary) {
    Boolean desired = desiredState();
    if (desired != null)
      return desired;
    Field enabled = cfg == null ? null : RefUtil.field(cfg.getClass(),
                                                       "enabled");
    if (enabled != null)
      return RefUtil.readBooleanField(cfg, enabled, true) &&
          anyKeyBound(primary, secondary);
    return anyKeyBound(primary, secondary);
  }

  private static void applyState(Object cfg, KeyMapping primary,
                                 KeyMapping secondary, boolean on) {
    setConfigEnabled(cfg, on);
    setKeyBound(primary, on, false);
    setKeyBound(secondary, on, true);
    if (!on)
      killActiveZoom();
    Minecraft.getInstance().options.save();
  }

  private static void setKeyBound(KeyMapping key, boolean bound,
                                  boolean isSecondary) {
    if (key == null)
      return;
    if (bound) {
      InputConstants.Key restore =
          isSecondary ? savedSecondaryKey : savedZoomKey;
      if (restore == null || restore == InputConstants.UNKNOWN)
        restore = key.getDefaultKey();
      key.setKey(restore);
    } else {
      InputConstants.Key current = readCurrentKey(key);
      if (current != null && current != InputConstants.UNKNOWN) {
        if (isSecondary)
          savedSecondaryKey = current;
        else
          savedZoomKey = current;
      }
      drainKey(key);
      key.setKey(InputConstants.UNKNOWN);
    }
    refreshMappings();
  }

  private static void drainKey(KeyMapping key) {
    if (key == null)
      return;
    key.setDown(false);
    while (key.consumeClick()) {
    }
  }

  private static void killActiveZoom() {
    KeyMapping primary = findZoomKey("zoomify.key.zoom", "zoomKey");
    KeyMapping secondary =
        findZoomKey("zoomify.key.zoom.secondary", "secondaryZoomKey");
    drainKey(primary);
    drainKey(secondary);
  }

  private static boolean anyKeyBound(KeyMapping a, KeyMapping b) {
    return (a != null && !a.isUnbound()) || (b != null && !b.isUnbound());
  }

  private static InputConstants.Key readCurrentKey(KeyMapping key) {
    RefUtil.MethodRef getKey = RefUtil.method(key.getClass(), "getKey");
    if (getKey != null) {
      Object val = getKey.invoke(key);
      if (val instanceof InputConstants.Key k)
        return k;
    }
    return null;
  }

  private static void refreshMappings() {
    RefUtil.MethodRef reset =
        RefUtil.staticMethod("net.minecraft.client.KeyMapping", "resetMapping");
    if (reset != null)
      reset.invokeStatic();
    Minecraft mc = Minecraft.getInstance();
    if (mc != null && mc.options != null)
      mc.options.save();
  }

  private static KeyMapping findZoomKey(String translationKey,
                                        String fieldName) {
    Class<?> cls = RefUtil.classForName("dev.isxander.zoomify.Zoomify");
    if (cls != null) {
      Field f = RefUtil.field(cls, fieldName);
      Object val = f == null ? null : RefUtil.readField(null, f);
      if (val instanceof KeyMapping km)
        return km;
    }
    for (KeyMapping km : Minecraft.getInstance().options.keyMappings) {
      if (km != null && translationKey.equalsIgnoreCase(km.getName()))
        return km;
    }
    return null;
  }

  private static Object zoomifySettings() {
    return RefUtil.staticField(
        "dev.isxander.zoomify.config.ZoomifySettings", "INSTANCE");
  }

  private static void setConfigEnabled(Object cfg, boolean enabled) {
    if (cfg == null)
      return;
    RefUtil.writeField(cfg, RefUtil.field(cfg.getClass(), "enabled"),
                       enabled);
    RefUtil.invoke(RefUtil.method(cfg.getClass(), "save"), cfg);
  }
}
