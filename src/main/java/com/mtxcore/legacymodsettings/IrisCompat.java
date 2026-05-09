package com.mtxcore.legacymodsettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.Iris;
import net.minecraft.network.chat.Component;

public class IrisCompat {

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!isIrisLoaded())
      return;
    if (!ModSettingsConfig.get().showAdvancedGraphicalEffects)
      return;

    if (!ModSettingsCompat.matchesSectionWithLegacyMerge(
            section, ModSettingsCompat.Section.GRAPHICS))
      return;

    String anchor =
        "render clouds|custom skin animation|smooth lighting|brightness|"
        + "gamma|preset";

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        anchor,
        ()
            -> Component.literal("Advanced Graphical Effects"),
        ()
            -> setAdvancedGraphicalEffectsEnabled(!areShadersEnabled()),
        IrisCompat::areShadersEnabled,
        ()
            -> Component.literal(
                "Turn shader-based graphical effects on or off.")));
  }

  static void setAdvancedGraphicalEffectsEnabled(boolean enable) {
    if (!isIrisLoaded())
      return;

    if (enable) {
      ModSettingsConfig cfg = ModSettingsConfig.get();
      if (cfg.irisShaderMode == ModSettingsConfig.IrisShaderMode.SPECIFIC &&
          cfg.irisSpecificShader != null && !cfg.irisSpecificShader.isBlank()) {
        applySpecificShaderPack(cfg.irisSpecificShader);
      }
    }

    setShadersEnabled(enable);
  }

  private static void applySpecificShaderPack(String packName) {
    try {
      Object api = net.irisshaders.iris.api.v0.IrisApi.getInstance();
      Object config = api.getClass().getMethod("getConfig").invoke(api);
      if (config == null)
        return;

      for (String methodName : new String[] {
               "setShaderPack",
               "setShaderPackName",
               "setSelectedShaderPack",
           }) {
        try {
          var method = config.getClass().getMethod(methodName, String.class);
          method.setAccessible(true);
          method.invoke(config, packName);
          return;
        } catch (NoSuchMethodException ignored) {
        }
      }
    } catch (Exception e) {
      CompatDebug.log("Could not set specific Iris shader pack '{}': {}",
                      packName, e.getMessage());
    }
  }

  public static boolean isIrisLoaded() {
    return FabricLoader.getInstance().isModLoaded("iris");
  }

  public static boolean areShadersEnabled() {
    if (!isIrisLoaded())
      return false;
    return net.irisshaders.iris.api.v0.IrisApi.getInstance()
        .getConfig()
        .areShadersEnabled();
  }

  public static void setShadersEnabled(boolean enable) {
    if (!isIrisLoaded())
      return;
    net.irisshaders.iris.api.v0.IrisApi.getInstance()
        .getConfig()
        .setShadersEnabledAndApply(enable);
  }

  public static List<String> listShaderPacks() {
    if (!isIrisLoaded())
      return new ArrayList<>();
    try {
      Path dir = Iris.getShaderpacksDirectory();
      if (dir == null || !Files.isDirectory(dir))
        return new ArrayList<>();
      List<String> result = new ArrayList<>();
      try (var stream = Files.list(dir)) {
        stream.filter(Iris::isValidToShowPack)
            .map(p -> p.getFileName().toString())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .forEach(result::add);
      }
      return result;
    } catch (Exception e) {
      CompatDebug.log("Could not list shader packs: {}", e.getMessage());
      return new ArrayList<>();
    }
  }
}
