package com.mtxcore.unifiedlegacysettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

final class NotEnoughAnimationsCompat {

  private static final String[] ANIMATION_FIELDS = {
      "enableEatDrinkAnimation",
      "enableLadderAnimation",
      "enableCrawlingAnimation",
      "enableRowBoatAnimation",
      "enableHorseAnimation",
      "tweakElytraAnimation",
      "petAnimation",
      "burningAnimation",
      "enableInWorldMapRendering",
      "itemSwapAnimation",
      "fallingAnimation",
      "freezingAnimation",
  };

  private NotEnoughAnimationsCompat() {}

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (section != ModSettingsCompat.Section.ADVANCED_GRAPHICS)
      return;
    if (!ModSettingsConfig.get().showNeaAnimations)
      return;
    if (!RefUtil.isModLoaded("notenoughanimations"))
      return;

    Object cfg = RefUtil.staticField(
        "dev.tr7zw.notenoughanimations.versionless.NEABaseMod", "config");
    if (cfg == null)
      return;

    List<Field> fields = new ArrayList<>();
    for (String name : ANIMATION_FIELDS) {
      Field field = RefUtil.field(cfg.getClass(), name);
      if (field != null)
        fields.add(field);
    }
    if (fields.isEmpty())
      return;

    Field representative = fields.get(0);
    entries.add(ModSettingsCompat.Entry.toggleBefore(
        "enhanced item translucency",
        ()
            -> Component.literal("Third-Person Animations"),
        ()
            -> {
          boolean next = !RefUtil.readBooleanField(cfg, representative, true);
          for (Field field : fields)
            RefUtil.writeField(cfg, field, next);
          saveConfig();
        },
        ()
            -> RefUtil.readBooleanField(cfg, representative, true),
        ()
            -> Component.literal("Play extra third-person animations for "
                                 + "actions and movement.")));
  }

  private static void saveConfig() {
    try {
      List<ClientModInitializer> eps =
          FabricLoader.getInstance().getEntrypoints("client",
                                                    ClientModInitializer.class);
      for (ClientModInitializer ep : eps) {
        if (ep.getClass().getName().startsWith(
                "dev.tr7zw.notenoughanimations")) {
          Method writeConfig = ep.getClass().getMethod("writeConfig");
          writeConfig.setAccessible(true);
          writeConfig.invoke(ep);
          return;
        }
      }
    } catch (Exception ignored) {
      // The toggle still works for the running game; save support depends on
      // the NEA entrypoint shape for the installed version.
    }
  }
}
