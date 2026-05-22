package com.mtxcore.unifiedlegacysettings;

import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class AdvancementScreenshotCompat {

  private static final String MOD_ID = "advancementscreenshot";
  private static final String CONFIG_CLASS =
      "com.natamus.advancementscreenshot_common_fabric.config.ConfigHandler";
  private static final String UTIL_CLASS =
      "com.natamus.advancementscreenshot_common_fabric.util.Util";
  private static final String ANCHOR =
      "show screenshot toasts|display game messages|autosave countdown";
  private static final int[] DELAY_PRESETS = {0, 20, 60, 100};

  private AdvancementScreenshotCompat() {}

  static void addEntries(ModSettingsCompat.Section section,
                         List<ModSettingsCompat.Entry> entries) {
    if (!ModSettingsCompat.matchesUserInterfaceSection(section))
      return;
    if (!ModSettingsConfig.get().showAdvancementScreenshot)
      return;
    if (!RefUtil.isModLoaded(MOD_ID))
      return;

    entries.add(ModSettingsCompat.Entry.toggleBefore(
        ANCHOR,
        ()
            -> Component.literal("Take Screenshot on Achievement"),
        AdvancementScreenshotCompat::toggleAchievementScreenshots,
        AdvancementScreenshotCompat::shouldTakeScreenshotOnAchievement,
        ()
            -> Component.literal(
                "Automatically save a screenshot when an advancement appears.")));

    if (field("showScreenshotTakenMessage") != null) {
      entries.add(ModSettingsCompat.Entry.toggleBefore(
          ANCHOR,
          ()
              -> Component.literal("Screenshot Taken Message"),
          AdvancementScreenshotCompat::toggleScreenshotTakenMessage,
          AdvancementScreenshotCompat::isScreenshotTakenMessageEnabled,
          ()
              -> Component.literal(
                  "Show the normal screenshot saved chat message.")));
    }

    if (field("takeScreenshotTickDelay") != null) {
      entries.add(ModSettingsCompat.Entry.cycleBefore(
          ANCHOR,
          ()
              -> Component.literal("Screenshot Delay: " + formattedDelay()),
          AdvancementScreenshotCompat::cycleScreenshotDelay,
          ()
              -> Component.literal(
                  "Wait after the advancement appears before taking the screenshot.")));
    }
  }

  public static boolean shouldTakeScreenshotOnAchievement() {
    return ModSettingsConfig.get().advancementScreenshotEnabled;
  }

  static void setTakeScreenshotOnAchievement(boolean enabled) {
    ModSettingsConfig.mutateAndSave(
        cfg -> cfg.advancementScreenshotEnabled = enabled);
    if (!enabled)
      clearPendingScreenshot();
  }

  private static void toggleAchievementScreenshots() {
    setTakeScreenshotOnAchievement(!shouldTakeScreenshotOnAchievement());
  }

  private static boolean isScreenshotTakenMessageEnabled() {
    Object value = RefUtil.readField(null, field("showScreenshotTakenMessage"));
    return RefUtil.asBool(value, false);
  }

  private static void toggleScreenshotTakenMessage() {
    setBoolean("showScreenshotTakenMessage",
               !isScreenshotTakenMessageEnabled());
  }

  private static void cycleScreenshotDelay() {
    int current = getDelayTicks();
    int next = DELAY_PRESETS[0];
    for (int preset : DELAY_PRESETS) {
      if (preset > current) {
        next = preset;
        break;
      }
    }
    setInt("takeScreenshotTickDelay", next, 0, 100);
  }

  private static String formattedDelay() {
    int ticks = getDelayTicks();
    if (ticks == 0)
      return "Instant";
    if (ticks == 20)
      return "1s";
    if (ticks == 60)
      return "3s";
    return "5s";
  }

  private static int getDelayTicks() {
    Object value = RefUtil.readField(null, field("takeScreenshotTickDelay"));
    int raw = value instanceof Number n ? n.intValue() : 20;
    return normalizeDelayTicks(raw);
  }

  private static int normalizeDelayTicks(int ticks) {
    int clamped = Math.max(0, Math.min(100, ticks));
    if (clamped == 0)
      return 0;
    if (clamped <= 20)
      return 20;
    if (clamped <= 60)
      return 60;
    return 100;
  }

  private static void setBoolean(String name, boolean value) {
    Field field = field(name);
    if (field == null)
      return;
    RefUtil.writeField(null, field, value);
    saveConfig();
  }

  private static void setInt(String name, int value, int min, int max) {
    Field field = field(name);
    if (field == null)
      return;
    RefUtil.writeField(null, field, Math.max(min, Math.min(max, value)));
    saveConfig();
  }

  public static void clearPendingScreenshot() {
    if (!RefUtil.isModLoaded(MOD_ID))
      return;
    RefUtil.writeField(null, RefUtil.field(RefUtil.classForName(UTIL_CLASS),
                                           "takescreenshot"),
                       false);
    RefUtil.writeField(null, RefUtil.field(RefUtil.classForName(UTIL_CLASS),
                                           "cooldown"),
                       -1);
  }

  private static void saveConfig() {
    RefUtil.MethodRef write =
        RefUtil.staticMethod("com.natamus.collective_common_fabric.config.DuskConfig",
                             "write", String.class);
    RefUtil.invoke(write, null, MOD_ID);
  }

  private static Field field(String name) {
    return RefUtil.field(RefUtil.classForName(CONFIG_CLASS), name);
  }
}
