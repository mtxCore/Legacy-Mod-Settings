package com.mtxcore.unifiedlegacysettings;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class ZoomCompat {

  private static InputConstants.Key lastZoomKey;
  private static InputConstants.Key lastSecondaryZoomKey;
  private static Boolean zoomDesired;

  private ZoomCompat() {}

  private static Boolean desiredZoomState() {
    if (zoomDesired != null)
      return zoomDesired;
    return ModSettingsConfig.get().zoomEnabled;
  }

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!ModSettingsCompat.matchesSectionWithLegacyMerge(
            section, ModSettingsCompat.Section.ADVANCED_GAME_OPTIONS))
      return;
    if (!ModSettingsConfig.get().showZoom)
      return;

    tryZoomify(entries);
  }

  private static boolean tryZoomify(List<ModSettingsCompat.Entry> entries) {
    if (!RefUtil.isModLoaded("zoomify"))
      return false;

    Object cfg = RefUtil.staticField(
        "dev.isxander.zoomify.config.ZoomifySettings", "INSTANCE");
    if (cfg != null) {
      Field enabled = RefUtil.field(cfg.getClass(), "enabled");
      if (enabled != null) {
        RefUtil.MethodRef save = RefUtil.method(cfg.getClass(), "save");
        KeyMapping zoomKey = resolveZoomifyKeyMapping();
        KeyMapping secondaryZoomKey = resolveZoomifySecondaryKeyMapping();

        entries.add(ModSettingsCompat.Entry.toggleBefore(
            "maps with coordinates",
            ()
                -> Component.literal("Zoom"),
            ()
                -> {
              boolean next =
                  !getZoomEnabled(cfg, enabled, zoomKey, secondaryZoomKey);
              zoomDesired = next;
              RefUtil.persistModDesired(next, RefUtil.PersistKey.ZOOM);
              CompatDebug.log("Zoom toggle (Zoomify config path) -> {}", next);
              applyZoomifyState(cfg, enabled, save, zoomKey, secondaryZoomKey,
                                next);
              applyZoomifyCompanionSettings(next);
            },
            ()
                -> getZoomEnabled(cfg, enabled, zoomKey, secondaryZoomKey),
            ()
                -> Component.literal(
                    "Magnify your view while holding the zoom control.")));
        return true;
      }
    }

    KeyMapping zoomKey = resolveZoomifyKeyMapping();
    KeyMapping secondaryZoomKey = resolveZoomifySecondaryKeyMapping();
    if (zoomKey == null && secondaryZoomKey == null) {
      CompatDebug.log("ZoomCompat: could not resolve Zoomify key mapping");
      return false;
    }

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "maps with coordinates",
        ()
            -> Component.literal("Zoom"),
        ()
            -> {
          boolean next = !getZoomEnabled(null, null, zoomKey, secondaryZoomKey);
          zoomDesired = next;
          RefUtil.persistModDesired(next, RefUtil.PersistKey.ZOOM);
          CompatDebug.log("Zoom toggle (keybind fallback path) -> {}", next);
          applyZoomKeyState(zoomKey, next, false);
          applyZoomKeyState(secondaryZoomKey, next, true);
          forceZoomifyRuntimeState(next);
          applyZoomifyCompanionSettings(next);
          Minecraft.getInstance().options.save();
        },
        ()
            -> getZoomEnabled(null, null, zoomKey, secondaryZoomKey),
        () -> Component.literal("Enable or disable manual camera zoom.")));
    return true;
  }

  private static KeyMapping resolveZoomifyKeyMapping() {
    Class<?> zoomifyCls = RefUtil.classForName("dev.isxander.zoomify.Zoomify");
    Field zoomKeyField =
        zoomifyCls == null ? null : RefUtil.field(zoomifyCls, "zoomKey");
    Object fromField =
        zoomKeyField == null ? null : RefUtil.readField(null, zoomKeyField);
    if (fromField instanceof KeyMapping km)
      return km;

    for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
      if (mapping == null)
        continue;
      String name = mapping.getName();
      if (name != null && name.equalsIgnoreCase("zoomify.key.zoom"))
        return mapping;
    }
    return null;
  }

  private static KeyMapping resolveZoomifySecondaryKeyMapping() {
    Class<?> zoomifyCls = RefUtil.classForName("dev.isxander.zoomify.Zoomify");
    Field field = zoomifyCls == null
                      ? null
                      : RefUtil.field(zoomifyCls, "secondaryZoomKey");
    Object fromField = field == null ? null : RefUtil.readField(null, field);
    if (fromField instanceof KeyMapping km)
      return km;

    for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
      if (mapping == null)
        continue;
      String name = mapping.getName();
      if (name != null && name.equalsIgnoreCase("zoomify.key.zoom.secondary"))
        return mapping;
    }
    return null;
  }

  static void enforceRuntimeState() {
    Boolean desired = desiredZoomState();
    if (desired == null)
      return;

    zoomDesired = desired;

    Object cfg = RefUtil.staticField(
        "dev.isxander.zoomify.config.ZoomifySettings", "INSTANCE");
    if (cfg != null) {
      Field enabled = RefUtil.field(cfg.getClass(), "enabled");
      RefUtil.MethodRef save = RefUtil.method(cfg.getClass(), "save");
      KeyMapping zoomKey = resolveZoomifyKeyMapping();
      KeyMapping secondaryZoomKey = resolveZoomifySecondaryKeyMapping();
      if (enabled != null) {
        if (isZoomEnabled(cfg, enabled, zoomKey, secondaryZoomKey) !=
            zoomDesired) {
          CompatDebug.log("Enforcing zoom -> {}", zoomDesired);
          applyZoomifyState(cfg, enabled, save, zoomKey, secondaryZoomKey,
                            zoomDesired);
        }
      } else {
        if (isAnyZoomKeyBound(zoomKey, secondaryZoomKey) != zoomDesired) {
          CompatDebug.log("Enforcing zoom -> {}", zoomDesired);
          applyZoomKeyState(zoomKey, zoomDesired, false);
          applyZoomKeyState(secondaryZoomKey, zoomDesired, true);
          forceZoomifyRuntimeState(zoomDesired);
        }
      }
    }

    if (!zoomDesired) {
      forceZoomifyRuntimeState(false);
      applyZoomifyCompanionSettings(false);
    }
  }

  private static boolean isZoomEnabled(Object cfg, Field enabledField,
                                       KeyMapping zoomKey,
                                       KeyMapping secondaryZoomKey) {
    boolean cfgEnabled = RefUtil.readBooleanField(cfg, enabledField, true);
    return cfgEnabled && isAnyZoomKeyBound(zoomKey, secondaryZoomKey);
  }

  private static boolean getZoomEnabled(Object cfg, Field enabledField,
                                        KeyMapping zoomKey,
                                        KeyMapping secondaryZoomKey) {
    Boolean desired = desiredZoomState();
    if (desired != null)
      return desired;
    if (cfg != null && enabledField != null) {
      return isZoomEnabled(cfg, enabledField, zoomKey, secondaryZoomKey);
    }
    return isAnyZoomKeyBound(zoomKey, secondaryZoomKey);
  }

  private static void applyZoomifyState(Object cfg, Field enabledField,
                                        RefUtil.MethodRef save,
                                        KeyMapping zoomKey,
                                        KeyMapping secondaryZoomKey,
                                        boolean enabled) {
    RefUtil.writeField(cfg, enabledField, enabled);
    applyZoomKeyState(zoomKey, enabled, false);
    applyZoomKeyState(secondaryZoomKey, enabled, true);
    forceZoomifyRuntimeState(enabled);
    RefUtil.invoke(save, cfg);
  }

  private static void applyZoomKeyState(KeyMapping zoomKey, boolean enabled,
                                        boolean secondary) {
    if (zoomKey == null)
      return;
    if (enabled) {
      InputConstants.Key remembered =
          secondary ? lastSecondaryZoomKey : lastZoomKey;
      InputConstants.Key restore =
          (remembered == null || remembered == InputConstants.UNKNOWN)
              ? zoomKey.getDefaultKey()
              : remembered;
      zoomKey.setKey(restore);
      refreshKeyMappings();
      return;
    }

    // Clear any latched key state/click queue before unbinding so HOLD/TOGGLE
    // modes cannot continue zooming after disable.
    clearKeyInputState(zoomKey);

    InputConstants.Key current = readCurrentKey(zoomKey);
    if (current != null && current != InputConstants.UNKNOWN) {
      if (secondary)
        lastSecondaryZoomKey = current;
      else
        lastZoomKey = current;
    }
    zoomKey.setKey(InputConstants.UNKNOWN);
    refreshKeyMappings();
  }

  private static InputConstants.Key readCurrentKey(KeyMapping key) {
    if (key == null)
      return null;

    RefUtil.MethodRef getKey = RefUtil.method(key.getClass(), "getKey");
    if (getKey != null) {
      Object value = getKey.invoke(key);
      if (value instanceof InputConstants.Key inputKey)
        return inputKey;
    }

    Field keyField = RefUtil.field(key.getClass(), "key");
    Object value = keyField == null ? null : RefUtil.readField(key, keyField);
    if (value instanceof InputConstants.Key inputKey)
      return inputKey;
    return null;
  }

  private static void refreshKeyMappings() {
    RefUtil.MethodRef reset =
        RefUtil.staticMethod("net.minecraft.client.KeyMapping", "resetMapping");
    if (reset != null)
      reset.invokeStatic();

    Minecraft mc = Minecraft.getInstance();
    if (mc != null && mc.options != null)
      mc.options.save();
  }

  private static boolean isAnyZoomKeyBound(KeyMapping zoomKey,
                                           KeyMapping secondaryZoomKey) {
    return ((zoomKey != null && !zoomKey.isUnbound()) ||
            (secondaryZoomKey != null && !secondaryZoomKey.isUnbound()));
  }

  private static void clearKeyInputState(KeyMapping key) {
    if (key == null)
      return;
    key.setDown(false);
    while (key.consumeClick()) {
      // Drain queued clicks from toggle-style handling.
    }
  }

  private static void forceZoomifyRuntimeState(boolean enabled) {
    if (enabled)
      return;

    Object instance =
        RefUtil.staticField("dev.isxander.zoomify.Zoomify", "INSTANCE");
    if (instance == null)
      return;

    Field zooming = RefUtil.field(instance.getClass(), "zooming");
    Field secondaryZooming =
        RefUtil.field(instance.getClass(), "secondaryZooming");
    Field scrollSteps = RefUtil.field(instance.getClass(), "scrollSteps");
    Field zoomKeyField = RefUtil.field(instance.getClass(), "zoomKey");
    Field secondaryZoomKeyField =
        RefUtil.field(instance.getClass(), "secondaryZoomKey");
    Field scrollZoomInField =
        RefUtil.field(instance.getClass(), "scrollZoomIn");
    Field scrollZoomOutField =
        RefUtil.field(instance.getClass(), "scrollZoomOut");
    if (zooming != null)
      RefUtil.writeField(instance, zooming, false);
    if (secondaryZooming != null)
      RefUtil.writeField(instance, secondaryZooming, false);
    if (scrollSteps != null)
      RefUtil.writeField(instance, scrollSteps, 0);

    Object zoomKeyObj =
        zoomKeyField == null ? null : RefUtil.readField(instance, zoomKeyField);
    if (zoomKeyObj instanceof KeyMapping key)
      clearKeyInputState(key);

    Object secondaryKeyObj =
        secondaryZoomKeyField == null
            ? null
            : RefUtil.readField(instance, secondaryZoomKeyField);
    if (secondaryKeyObj instanceof KeyMapping key)
      clearKeyInputState(key);

    Object scrollInObj = scrollZoomInField == null
                             ? null
                             : RefUtil.readField(instance, scrollZoomInField);
    if (scrollInObj instanceof KeyMapping key)
      clearKeyInputState(key);

    Object scrollOutObj = scrollZoomOutField == null
                              ? null
                              : RefUtil.readField(instance, scrollZoomOutField);
    if (scrollOutObj instanceof KeyMapping key)
      clearKeyInputState(key);

    Field zoomHelperField = RefUtil.field(instance.getClass(), "zoomHelper");
    Object zoomHelper = zoomHelperField == null
                            ? null
                            : RefUtil.readField(instance, zoomHelperField);
    if (zoomHelper != null) {
      RefUtil.MethodRef setToZero = RefUtil.method(
          zoomHelper.getClass(), "setToZero", boolean.class, boolean.class);
      if (setToZero != null)
        setToZero.invoke(zoomHelper, true, true);
    }

    Field secondaryZoomHelperField =
        RefUtil.field(instance.getClass(), "secondaryZoomHelper");
    Object secondaryHelper =
        secondaryZoomHelperField == null
            ? null
            : RefUtil.readField(instance, secondaryZoomHelperField);
    if (secondaryHelper != null) {
      RefUtil.MethodRef setToZero =
          RefUtil.method(secondaryHelper.getClass(), "setToZero", boolean.class,
                         boolean.class);
      if (setToZero != null)
        setToZero.invoke(secondaryHelper, true, true);
    }
  }

  private static void applyZoomifyCompanionSettings(boolean enabled) {
    Object companion = RefUtil.staticField(
        "dev.isxander.zoomify.config.ZoomifySettings", "Companion");
    if (companion == null)
      return;

    RefUtil.MethodRef getKeybindScrolling =
        RefUtil.method(companion.getClass(), "get_keybindScrolling");
    Object keybindScrollingEntry = getKeybindScrolling == null
                                       ? null
                                       : getKeybindScrolling.invoke(companion);
    setConfigEntryValue(keybindScrollingEntry, false);

    RefUtil.MethodRef getSpyglassBehaviour =
        RefUtil.method(companion.getClass(), "getSpyglassBehaviour");
    Object spyglassEntry = getSpyglassBehaviour == null
                               ? null
                               : getSpyglassBehaviour.invoke(companion);
    Object combine = resolveZoomifySpyglassCombine();
    if (combine != null && !enabled) {
      setConfigEntryValue(spyglassEntry, combine);
    }

    RefUtil.MethodRef saveToFile =
        RefUtil.method(companion.getClass(), "saveToFile");
    RefUtil.invoke(saveToFile, companion);
  }

  private static Object resolveZoomifySpyglassCombine() {
    Class<?> spyglassEnum =
        RefUtil.classForName("dev.isxander.zoomify.config.SpyglassBehaviour");
    return spyglassEnum == null ? null
                                : RefUtil.enumConstant(spyglassEnum, "COMBINE");
  }

  private static void setConfigEntryValue(Object entry, Object value) {
    if (entry == null)
      return;

    RefUtil.MethodRef setObject =
        RefUtil.method(entry.getClass(), "set", Object.class);
    if (setObject != null) {
      setObject.invoke(entry, value);
      return;
    }

    RefUtil.MethodRef setValueObject =
        RefUtil.method(entry.getClass(), "setValue", Object.class);
    if (setValueObject != null) {
      setValueObject.invoke(entry, value);
      return;
    }

    java.lang.reflect.Field pendingValue =
        RefUtil.field(entry.getClass(), "pendingValue");
    if (pendingValue != null) {
      RefUtil.writeField(entry, pendingValue, value);
    }
  }
}
