package com.mtxcore.legacymodsettings;

import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.components.AbstractSliderButton;
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
                  -> new GrassDetailSlider(cfg, values, getMode, setMode, save),
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
            },
            ()
                -> RefUtil.invokeBoolean(hasBetterLayer, cfg, true),
            ()
                -> Component.literal(
                    "Blend snow layers more smoothly with nearby blocks.")));
      }
    }
  }

  private static final class GrassDetailSlider extends AbstractSliderButton {

    private final Object cfg;
    private final Object[] values;
    private final RefUtil.MethodRef getMode;
    private final RefUtil.MethodRef setMode;
    private final RefUtil.MethodRef save;

    private GrassDetailSlider(Object cfg, Object[] values,
                              RefUtil.MethodRef getMode,
                              RefUtil.MethodRef setMode,
                              RefUtil.MethodRef save) {
      super(0, 0, 200, 20, Component.empty(), 0.0D);
      this.cfg = cfg;
      this.values = Arrays.copyOf(values, values.length);
      this.getMode = getMode;
      this.setMode = setMode;
      this.save = save;
      this.value = modeToSliderValue(getMode.invoke(cfg));
      updateMessage();
    }

    @Override
    protected void updateMessage() {
      Object mode = sliderValueToMode(this.value);
      this.setMessage(
          Component.literal("Grass Detail: " + RefUtil.prettyEnumName(mode)));
    }

    @Override
    protected void applyValue() {
      Object next = sliderValueToMode(this.value);
      if (next == null)
        return;
      setMode.invoke(cfg, next);
      RefUtil.invoke(save, cfg);
      this.value = modeToSliderValue(getMode.invoke(cfg));
      updateMessage();
    }

    private double modeToSliderValue(Object mode) {
      if (values.length <= 1)
        return 0.0D;
      int idx = indexOfMode(mode);
      if (idx < 0)
        idx = 0;
      return (double)idx / (double)(values.length - 1);
    }

    private Object sliderValueToMode(double slider) {
      if (values.length == 0)
        return null;
      int idx = (int)Math.round(slider * (values.length - 1));
      if (idx < 0)
        idx = 0;
      if (idx >= values.length)
        idx = values.length - 1;
      return values[idx];
    }

    private int indexOfMode(Object mode) {
      if (mode == null)
        return -1;
      for (int i = 0; i < values.length; i++) {
        if (values[i] == mode || values[i].equals(mode))
          return i;
      }
      return -1;
    }
  }
}
