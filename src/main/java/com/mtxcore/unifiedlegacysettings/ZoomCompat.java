package com.mtxcore.unifiedlegacysettings;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

// Toggles Zoomify stuff on and off and make camera not get stuck
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

    Object cfg = RefUtil.staticField(
        "dev.isxander.zoomify.config.ZoomifySettings", "INSTANCE");
    Field enabled =
        cfg == null ? null : RefUtil.field(cfg.getClass(), "enabled");
    RefUtil.MethodRef save =
        cfg == null ? null : RefUtil.method(cfg.getClass(), "save");

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "maps with coordinates",
        ()
            -> Component.literal("Zoom"),
        ()
            -> {
          boolean next = !getEnabled(cfg, enabled, primary, secondary);
          zoomDesired = next;
          RefUtil.persistModDesired(next, RefUtil.PersistKey.ZOOM);
          applyState(cfg, enabled, save, primary, secondary, next);
        },
        ()
            -> getEnabled(cfg, enabled, primary, secondary),
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
    Object cfg = RefUtil.staticField(
        "dev.isxander.zoomify.config.ZoomifySettings", "INSTANCE");
    Field enabled =
        cfg == null
            ? null
            : RefUtil.field(cfg == null ? null : cfg.getClass(), "enabled");
    RefUtil.MethodRef save =
        cfg == null ? null : RefUtil.method(cfg.getClass(), "save");

    if (getEnabled(cfg, enabled, primary, secondary) != desired)
      applyState(cfg, enabled, save, primary, secondary, desired);

    if (!desired)
      killActiveZoom();
  }

  private static boolean getEnabled(Object cfg, Field enabled,
                                    KeyMapping primary, KeyMapping secondary) {
    Boolean desired = desiredState();
    if (desired != null)
      return desired;
    if (cfg != null && enabled != null)
      return RefUtil.readBooleanField(cfg, enabled, true) &&
          anyKeyBound(primary, secondary);
    return anyKeyBound(primary, secondary);
  }

  private static void applyState(Object cfg, Field enabled,
                                 RefUtil.MethodRef save, KeyMapping primary,
                                 KeyMapping secondary, boolean on) {
    if (cfg != null && enabled != null)
      RefUtil.writeField(cfg, enabled, on);
    setKeyBound(primary, on, false);
    setKeyBound(secondary, on, true);
    if (!on)
      killActiveZoom();
    if (cfg != null)
      RefUtil.invoke(save, cfg);
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

  // Drain any queued clicks so HOLD/TOGGLE modes stop zooming immediately.
  private static void drainKey(KeyMapping key) {
    key.setDown(false);
    // noinspection StatementWithEmptyBody
    while (key.consumeClick()) {
    }
  }

  private static void killActiveZoom() {
    Object instance =
        RefUtil.staticField("dev.isxander.zoomify.Zoomify", "INSTANCE");
    if (instance == null)
      return;

    for (String name : new String[] {"zooming", "secondaryZooming"}) {
      Field f = RefUtil.field(instance.getClass(), name);
      if (f != null)
        RefUtil.writeField(instance, f, false);
    }
    Field scrollSteps = RefUtil.field(instance.getClass(), "scrollSteps");
    if (scrollSteps != null)
      RefUtil.writeField(instance, scrollSteps, 0);

    // Drain any live KeyMappings attached to the Zoomify instance
    for (String name : new String[] {"zoomKey", "secondaryZoomKey",
                                     "scrollZoomIn", "scrollZoomOut"}) {
      Field f = RefUtil.field(instance.getClass(), name);
      Object val = f == null ? null : RefUtil.readField(instance, f);
      if (val instanceof KeyMapping km)
        drainKey(km);
    }

    // If there's a zoom helper, snap it to zero
    for (String name : new String[] {"zoomHelper", "secondaryZoomHelper"}) {
      Field f = RefUtil.field(instance.getClass(), name);
      Object helper = f == null ? null : RefUtil.readField(instance, f);
      if (helper == null)
        continue;
      RefUtil.MethodRef setToZero = RefUtil.method(
          helper.getClass(), "setToZero", boolean.class, boolean.class);
      if (setToZero != null)
        setToZero.invoke(helper, true, true);
    }
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
    Field f = RefUtil.field(key.getClass(), "key");
    Object val = f == null ? null : RefUtil.readField(key, f);
    return val instanceof InputConstants.Key k ? k : null;
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

  // Check the static field on Zoomify's class, then fall back to scanning all
  // keybinds
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
}
