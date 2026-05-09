package com.mtxcore.unifiedlegacysettings;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.network.chat.Component;

final class LambDynLightsCompat {

  private LambDynLightsCompat() {}

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!ModSettingsCompat.matchesSectionWithLegacyMerge(
            section, ModSettingsCompat.Section.GAME_OPTIONS))
      return;
    if (!ModSettingsConfig.get().showDynamicLighting)
      return;
    if (!RefUtil.isModLoaded("lambdynlights"))
      return;

    RefUtil.MethodRef getMod = RefUtil.staticMethod(
        "dev.lambdaurora.lambdynlights.LambDynLights", "get");
    if (getMod == null)
      return;

    Object mod = getMod.invokeStatic();
    if (mod == null)
      return;

    Object cfg = RefUtil.instanceField(mod, "config");
    if (cfg == null)
      return;

    Field forceRefresh = RefUtil.field(mod.getClass(), "shouldForceRefresh");
    Object engine = RefUtil.instanceField(mod, "engine");
    RefUtil.MethodRef resetSize =
        engine == null ? null : RefUtil.method(engine.getClass(), "resetSize");
    RefUtil.MethodRef removeLightSources =
        RefUtil.method(mod.getClass(), "removeLightSources",
                       java.util.function.Predicate.class);

    Class<?> modeClass =
        RefUtil.classForName("dev.lambdaurora.lambdynlights.DynamicLightsMode");
    RefUtil.MethodRef getMode =
        RefUtil.method(cfg.getClass(), "getDynamicLightsMode");
    RefUtil.MethodRef setMode =
        RefUtil.method(cfg.getClass(), "setDynamicLightsMode", modeClass);
    RefUtil.MethodRef save = RefUtil.method(cfg.getClass(), "save");
    if (getMode == null || setMode == null || modeClass == null)
      return;

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "view bobbing",
        ()
            -> Component.literal("Dynamic Lighting"),
        ()
            -> {
          Object current = getMode.invoke(cfg);
          Object next = resolveToggleValue(modeClass, current);
          if (next == null)
            return;
          setMode.invoke(cfg, next);
          RefUtil.invoke(save, cfg);
          if (forceRefresh != null)
            RefUtil.writeField(mod, forceRefresh, true);
          RefUtil.invoke(resetSize, engine);
          if (!isEnabled(next) && removeLightSources != null) {
            RefUtil.invoke(
                removeLightSources, mod,
                (java.util.function.Predicate<Object>)ignored -> true);
          }
        },
        ()
            -> isEnabled(getMode.invoke(cfg)),
        ()
            -> Component.literal("Brightens nearby areas with light from "
                                 + "held and dropped items.")));
  }

  private static boolean isEnabled(Object mode) {
    return !(mode instanceof Enum<?> e) || !"OFF".equalsIgnoreCase(e.name());
  }

  private static Object resolveToggleValue(Class<?> modeClass, Object current) {
    Object off = RefUtil.enumConstant(modeClass, "OFF");
    Object on = RefUtil.enumConstant(modeClass, "FANCY");
    if (on == null)
      on = RefUtil.enumConstant(modeClass, "FAST");
    if (on == null) {
      Object[] constants = modeClass.getEnumConstants();
      if (constants != null && constants.length > 0)
        on = constants[0];
    }
    if (current instanceof Enum<?> e && "OFF".equalsIgnoreCase(e.name()))
      return on;
    return off == null ? on : off;
  }
}
