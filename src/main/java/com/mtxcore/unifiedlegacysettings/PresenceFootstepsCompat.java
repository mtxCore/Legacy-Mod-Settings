package com.mtxcore.unifiedlegacysettings;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

final class PresenceFootstepsCompat {

  private interface ToggleAccessor {
    boolean isEnabled();
    void setEnabled(boolean enabled);
  }

  private PresenceFootstepsCompat() {}

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!ModSettingsCompat.matchesSectionWithLegacyMerge(
            section, ModSettingsCompat.Section.AUDIO))
      return;
    if (!ModSettingsConfig.get().showPresenceFootsteps)
      return;
    if (!RefUtil.isModLoaded("presencefootsteps"))
      return;

    ToggleAccessor toggle = resolveToggleAccessor();
    if (toggle == null) {
      return;
    }

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "inventory hover focus sound",
        ()
            -> Component.literal("Presence Footsteps"),
        ()
            -> {
          boolean next = !toggle.isEnabled();
          try {
            toggle.setEnabled(next);
          } catch (Throwable t) {
            UnifiedLegacySettings.LOGGER.error(
                "[ULS] Presence Footsteps toggle failed", t);
          }
          removePresenceFootstepsToasts();
        },
        toggle::isEnabled,
        ()
            -> Component.literal(
                "Play rich, surface-aware footstep sounds as you walk.")));
  }

  private static int removePresenceFootstepsToasts() {
    int removedCount = 0;
    try {
      Object toastManager;
      try {
        toastManager = Minecraft.getInstance()
                           .getClass()
                           .getMethod("getToastManager")
                           .invoke(Minecraft.getInstance());
      } catch (Exception e) {
        try {
          toastManager = Minecraft.getInstance()
                             .getClass()
                             .getMethod("getToasts")
                             .invoke(Minecraft.getInstance());
        } catch (Exception e2) {
          toastManager = null;
        }
      }
      if (toastManager == null)
        return 0;

      for (Field field : toastManager.getClass().getDeclaredFields()) {
        field.setAccessible(true);
        Object value = field.get(toastManager);
        if (value instanceof Map<?, ?> map) {
          removedCount += removePresenceToasts(map.entrySet().iterator(), true);
        } else if (value instanceof List<?> list) {
          removedCount += removePresenceToasts(list.iterator(), false);
        }
      }
    } catch (Throwable t) {
      UnifiedLegacySettings.LOGGER.error(
          "[ULS] Failed to remove Presence Footsteps toasts", t);
    }
    return removedCount;
  }

  private static int removePresenceToasts(Iterator<?> iterator,
                                          boolean useEntryValue) {
    int removed = 0;
    while (iterator.hasNext()) {
      Object raw = iterator.next();
      Object value = raw;
      if (useEntryValue && raw instanceof Map.Entry<?, ?> entry) {
        value = entry.getValue();
      }

      if (isPresenceFootstepsToast(value)) {
        iterator.remove();
        removed++;
      }
    }
    return removed;
  }

  private static boolean isPresenceFootstepsToast(Object toast) {
    if (toast == null)
      return false;
    String text = toast.toString().toLowerCase(Locale.ROOT);
    return (text.contains("presence footsteps") ||
            text.contains("presencefootsteps") ||
            text.contains("sounds enabled") ||
            text.contains("sounds disabled"));
  }

  private static ToggleAccessor resolveToggleAccessor() {
    ToggleAccessor modern = resolveModernAccessor();
    if (modern != null)
      return modern;
    ToggleAccessor legacy = resolveLegacyAccessor();
    if (legacy != null)
      return legacy;
    return null;
  }

  private static ToggleAccessor resolveModernAccessor() {
    RefUtil.MethodRef getInstance = RefUtil.staticMethod(
        "eu.ha3.presencefootsteps.PresenceFootsteps", "getInstance");
    if (getInstance == null)
      return null;

    Object mod = getInstance.invokeStatic();
    if (mod == null)
      return null;

    RefUtil.MethodRef getConfig = RefUtil.method(mod.getClass(), "getConfig");
    if (getConfig == null)
      return null;

    Object cfg = getConfig.invoke(mod);
    if (cfg == null)
      return null;
    RefUtil.MethodRef getEnabled = RefUtil.method(cfg.getClass(), "getEnabled");
    RefUtil.MethodRef setDisabled =
        RefUtil.method(cfg.getClass(), "setDisabled", boolean.class);

    Field disabledField = RefUtil.field(cfg.getClass(), "disabled");
    Object disabledSetting =
        disabledField == null ? null : RefUtil.readField(cfg, disabledField);
    RefUtil.MethodRef settingGet =
        disabledSetting == null
            ? null
            : RefUtil.method(disabledSetting.getClass(), "get");
    RefUtil.MethodRef settingSet =
        disabledSetting == null
            ? null
            : RefUtil.method(disabledSetting.getClass(), "set", boolean.class);
    RefUtil.MethodRef saveMethod = RefUtil.method(cfg.getClass(), "save");

    if (settingSet != null && saveMethod != null) {
      return new ToggleAccessor() {
        @Override
        public boolean isEnabled() {
          if (getEnabled != null)
            return RefUtil.invokeBoolean(getEnabled, cfg, true);
          Object value = settingGet == null ? null : settingGet.invoke(
              disabledSetting);
          return value instanceof Boolean disabled ? !disabled : true;
        }

        @Override
        public void setEnabled(boolean enabled) {
          settingSet.invoke(disabledSetting, !enabled);
          saveMethod.invoke(cfg);
          reloadEngine(mod);
        }
      };
    }

    if (getEnabled == null || setDisabled == null)
      return null;

    return new ToggleAccessor() {
      @Override
      public boolean isEnabled() {
        return RefUtil.invokeBoolean(getEnabled, cfg, true);
      }

      @Override
      public void setEnabled(boolean enabled) {
        setDisabled.invoke(cfg, !enabled);
        reloadEngine(mod);
      }
    };
  }

  private static void reloadEngine(Object mod) {
    Object engine = RefUtil.invoke(RefUtil.method(mod.getClass(), "getEngine"),
                                   mod);
    RefUtil.invoke(engine == null ? null : RefUtil.method(engine.getClass(),
                                                          "reload"),
                   engine);
  }

  private static ToggleAccessor resolveLegacyAccessor() {
    Object cfg = RefUtil.staticField("eu.ha3.presencefootsteps.config.PFConfig",
                                     "INSTANCE");
    if (cfg == null)
      return null;

    Field enabled = RefUtil.field(cfg.getClass(), "enabled");
    if (enabled == null)
      return null;

    RefUtil.MethodRef save = RefUtil.method(cfg.getClass(), "save");

    return new ToggleAccessor() {
      @Override
      public boolean isEnabled() {
        return RefUtil.readBooleanField(cfg, enabled, true);
      }

      @Override
      public void setEnabled(boolean value) {
        RefUtil.writeField(cfg, enabled, value);
        RefUtil.invoke(save, cfg);
      }
    };
  }
}
