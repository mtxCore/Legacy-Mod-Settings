package com.mtxcore.legacymodsettings;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class LambdaBetterGrassCompat {

  private LambdaBetterGrassCompat() {}

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!RefUtil.isModLoaded("lambdabettergrass"))
      return;

    RefUtil.MethodRef getMod = RefUtil.staticMethod(
        "dev.lambdaurora.lambdabettergrass.LambdaBetterGrass", "get");
    if (getMod == null)
      return;
    Object mod = getMod.invokeStatic();
    if (mod == null)
      return;
    Object cfg = RefUtil.instanceField(mod, "config");
    if (cfg == null)
      return;

    if (section == ModSettingsCompat.Section.ADVANCED_GRAPHICS &&
        ModSettingsConfig.get().showGrassDetail) {
      RefUtil.MethodRef getMode = RefUtil.method(cfg.getClass(), "getMode");
      Class<?> modeClass =
          RefUtil.classForName("dev.lambdaurora.lambdabettergrass.LBGMode");
      RefUtil.MethodRef setMode =
          RefUtil.method(cfg.getClass(), "setMode", modeClass);
      RefUtil.MethodRef save = RefUtil.method(cfg.getClass(), "save");

      if (getMode != null && setMode != null && modeClass != null &&
          modeClass.isEnum()) {
        Object[] values = modeClass.getEnumConstants();
        if (values != null && values.length > 0) {
          entries.add(ModSettingsCompat.Entry.customBefore(
              "display held item lighting", "Grass Detail",
              ()
                  -> createLegacyGrassDetailSlider(mod, cfg, values, getMode,
                                                   setMode, save),
              ()
                  -> Component.literal("Choose how detailed grass and snow "
                                       + "edges should appear.")));
        }
      }
    }

    if (section == ModSettingsCompat.Section.ADVANCED_GRAPHICS &&
        ModSettingsConfig.get().showSnowLayerBlending) {
      RefUtil.MethodRef hasBetterLayer =
          RefUtil.method(cfg.getClass(), "hasBetterLayer");
      RefUtil.MethodRef setBetterLayer =
          RefUtil.method(cfg.getClass(), "setBetterLayer", boolean.class);
      RefUtil.MethodRef save = RefUtil.method(cfg.getClass(), "save");
      if (hasBetterLayer != null && setBetterLayer != null) {
        entries.add(ModSettingsCompat.Entry.toggleBefore(
            "enhanced item translucency",
            ()
                -> Component.literal("Snow Layer Blending"),
            ()
                -> {
              boolean next = !RefUtil.invokeBoolean(hasBetterLayer, cfg, true);
              setBetterLayer.invoke(cfg, next);
              RefUtil.invoke(save, cfg);
              triggerRefresh(mod, cfg);
            },
            ()
                -> RefUtil.invokeBoolean(hasBetterLayer, cfg, true),
            ()
                -> Component.literal(
                    "Blend snow layers more smoothly with nearby blocks.")));
      }
    }
  }

  private static void triggerRefresh(Object mod, Object cfg) {
    if (cfg != null) {
      for (String methodName : new String[] {
               "reload",
               "reloadRenderer",
               "onConfigChanged",
               "onConfigChange",
           }) {
        RefUtil.MethodRef m = RefUtil.method(cfg.getClass(), methodName);
        if (m != null) {
          m.invoke(cfg);
          break;
        }
      }
    }

    if (mod != null) {
      for (String methodName : new String[] {
               "reload",
               "reloadRenderer",
               "onConfigChanged",
               "onConfigChange",
           }) {
        RefUtil.MethodRef m = RefUtil.method(mod.getClass(), methodName);
        if (m != null) {
          m.invoke(mod);
          break;
        }
      }
    }

    Minecraft mc = Minecraft.getInstance();
    if (mc != null) {
      if (mc.levelRenderer != null)
        mc.levelRenderer.allChanged();
      if (mc.options != null)
        mc.options.save();
    }
  }

  private static Object createLegacyGrassDetailSlider(Object mod, Object cfg,
                                                      Object[] values,
                                                      RefUtil.MethodRef getMode,
                                                      RefUtil.MethodRef setMode,
                                                      RefUtil.MethodRef save) {
    Class<?> sliderClass =
        RefUtil.classForName("wily.legacy.client.screen.LegacySliderButton");
    if (sliderClass == null)
      return null;

    Constructor<?> ctor = findCtorByParamCount(sliderClass, 10);
    if (ctor == null)
      return null;

    Object initial = getMode.invoke(cfg);
    if (initial == null)
      initial = values[0];

    Supplier<List<Object>> valueListSupplier =
        () -> Arrays.asList(Arrays.copyOf(values, values.length));

    Function<Object, Component> messageGetter = slider
        -> Component.literal("Grass Detail: " +
                             RefUtil.prettyEnumName(getLegacySliderObjectValue(
                                 slider, getMode.invoke(cfg))));

    Function<Object, Tooltip> tooltipSupplier = slider -> null;

    Consumer<Object> onChange = slider -> {
      Object next = getLegacySliderObjectValue(slider, getMode.invoke(cfg));
      if (next == null)
        return;
      setMode.invoke(cfg, next);
      RefUtil.invoke(save, cfg);
      triggerRefresh(mod, cfg);
    };

    Supplier<Object> currentSupplier = () -> getMode.invoke(cfg);

    try {
      ctor.setAccessible(true);
      return ctor.newInstance(0, 0, 200, 16, messageGetter, tooltipSupplier,
                              initial, valueListSupplier, onChange,
                              currentSupplier);
    } catch (Exception e) {
      CompatDebug.log(
          "Could not create LegacySliderButton for Grass Detail: {}",
          e.getMessage());
      return null;
    }
  }

  private static Object getLegacySliderObjectValue(Object slider,
                                                   Object fallback) {
    if (slider == null)
      return fallback;

    RefUtil.MethodRef getObjectValue =
        RefUtil.method(slider.getClass(), "getObjectValue");
    if (getObjectValue == null)
      return fallback;
    Object value = getObjectValue.invoke(slider);
    return value == null ? fallback : value;
  }

  private static Constructor<?> findCtorByParamCount(Class<?> type,
                                                     int paramCount) {
    for (Constructor<?> ctor : type.getDeclaredConstructors()) {
      if (ctor.getParameterCount() == paramCount)
        return ctor;
    }
    return null;
  }
}
