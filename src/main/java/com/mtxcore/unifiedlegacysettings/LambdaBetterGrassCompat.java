package com.mtxcore.unifiedlegacysettings;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

final class LambdaBetterGrassCompat {

  private LambdaBetterGrassCompat() {}

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (section != ModSettingsCompat.Section.ADVANCED_GRAPHICS)
      return;
    if (!RefUtil.isModLoaded("lambdabettergrass"))
      return;

    Object mod = RefUtil.invoke(
        RefUtil.staticMethod(
            "dev.lambdaurora.lambdabettergrass.LambdaBetterGrass", "get"),
        null);
    if (mod == null)
      return;

    Object cfg = RefUtil.instanceField(mod, "config");
    if (cfg == null)
      return;

    if (ModSettingsConfig.get().showGrassDetail)
      addGrassDetailEntry(entries, mod, cfg);

    if (ModSettingsConfig.get().showSnowLayerBlending)
      addSnowBlendingEntry(entries, mod, cfg);
  }

  private static void addGrassDetailEntry(List<ModSettingsCompat.Entry> entries,
                                          Object mod, Object cfg) {
    Class<?> modeClass =
        RefUtil.classForName("dev.lambdaurora.lambdabettergrass.LBGMode");
    RefUtil.MethodRef getMode = RefUtil.method(cfg.getClass(), "getMode");
    RefUtil.MethodRef setMode =
        modeClass == null
            ? null
            : RefUtil.method(cfg.getClass(), "setMode", modeClass);
    RefUtil.MethodRef save = RefUtil.method(cfg.getClass(), "save");

    if (getMode == null || setMode == null || modeClass == null ||
        !modeClass.isEnum())
      return;

    Object[] values = modeClass.getEnumConstants();
    if (values == null || values.length == 0)
      return;

    entries.add(ModSettingsCompat.Entry.customBefore(
        "display held item lighting", "Grass Detail",
        ()
            -> buildGrassSlider(mod, cfg, values, getMode, setMode, save),
        ()
            -> Component.literal(
                "Choose how detailed grass and snow edges should appear.")));
  }

  private static void
  addSnowBlendingEntry(List<ModSettingsCompat.Entry> entries, Object mod,
                       Object cfg) {
    RefUtil.MethodRef hasBetterLayer =
        RefUtil.method(cfg.getClass(), "hasBetterLayer");
    RefUtil.MethodRef setBetterLayer =
        RefUtil.method(cfg.getClass(), "setBetterLayer", boolean.class);
    RefUtil.MethodRef save = RefUtil.method(cfg.getClass(), "save");
    if (hasBetterLayer == null || setBetterLayer == null)
      return;

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "enhanced item translucency",
        ()
            -> Component.literal("Snow Layer Blending"),
        ()
            -> {
          boolean next = !RefUtil.invokeBoolean(hasBetterLayer, cfg, true);
          setBetterLayer.invoke(cfg, next);
          RefUtil.invoke(save, cfg);
          reloadRenderer(mod);
        },
        ()
            -> RefUtil.invokeBoolean(hasBetterLayer, cfg, true),
        ()
            -> Component.literal(
                "Blend snow layers more smoothly with nearby blocks.")));
  }

  private static void reloadRenderer(Object mod) {
    RefUtil.invoke(RefUtil.method(mod.getClass(), "reload"), mod);
  }

  private static Object buildGrassSlider(Object mod, Object cfg,
                                         Object[] values,
                                         RefUtil.MethodRef getMode,
                                         RefUtil.MethodRef setMode,
                                         RefUtil.MethodRef save) {
    Class<?> sliderClass =
        RefUtil.classForName("wily.legacy.client.screen.LegacySliderButton");
    if (sliderClass == null)
      return null;

    Constructor<?> ctor = findCtor(sliderClass, 10);
    if (ctor == null)
      return null;

    Object initial = getMode.invoke(cfg);
    if (initial == null)
      initial = values[0];

    Supplier<List<Object>> valueList =
        () -> Arrays.asList(Arrays.copyOf(values, values.length));
    Function<Object, Component> label = slider
        -> Component.literal(
            "Grass Detail: " +
            RefUtil.prettyEnumName(selectedSliderValue(
                slider, getMode.invoke(cfg))));
    Function<Object, Tooltip> noTooltip = slider -> null;
    Consumer<Object> onChange = slider -> {
      Object next = selectedSliderValue(slider, getMode.invoke(cfg));
      if (next == null)
        return;
      setMode.invoke(cfg, next);
      RefUtil.invoke(save, cfg);
      reloadRenderer(mod);
    };
    Supplier<Object> current = () -> getMode.invoke(cfg);

    try {
      ctor.setAccessible(true);
      return ctor.newInstance(0, 0, 200, 16, label, noTooltip, initial,
                              valueList, onChange, current);
    } catch (Exception e) {
      return null;
    }
  }

  private static Object selectedSliderValue(Object slider, Object current) {
    if (slider == null)
      return current;
    RefUtil.MethodRef get = RefUtil.method(slider.getClass(), "getObjectValue");
    if (get == null)
      return current;
    Object val = get.invoke(slider);
    return val != null ? val : current;
  }

  private static Constructor<?> findCtor(Class<?> cls, int paramCount) {
    for (Constructor<?> c : cls.getDeclaredConstructors())
      if (c.getParameterCount() == paramCount)
        return c;
    return null;
  }
}
