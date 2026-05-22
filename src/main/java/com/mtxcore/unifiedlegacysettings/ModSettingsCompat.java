package com.mtxcore.unifiedlegacysettings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ModSettingsCompat {

  public enum Section {
    GRAPHICS,
    ADVANCED_GRAPHICS,
    GAME_OPTIONS,
    ADVANCED_GAME_OPTIONS,
    AUDIO,
    ADVANCED_AUDIO,
    USER_INTERFACE,
    ADVANCED_USER_INTERFACE,
    OTHER,
  }

  public enum WidgetKind {
    TOGGLE,
    CYCLE,
    CUSTOM,
  }

  public record Entry(String insertBeforeText, String dedupeText,
                      Supplier<Component> label, Runnable onActivate,
                      BooleanSupplier selected, Supplier<Component> tooltip,
                      WidgetKind kind, Supplier<Object> customWidget) {
    public static Entry toggleBefore(String before, Supplier<Component> label,
                                     Runnable onActivate,
                                     BooleanSupplier selected,
                                     Supplier<Component> tooltip) {
      return new Entry(before, label.get().getString(), label, onActivate,
                       selected, tooltip, WidgetKind.TOGGLE, null);
    }

    public static Entry cycleBefore(String before, Supplier<Component> label,
                                    Runnable onActivate,
                                    Supplier<Component> tooltip) {
      return new Entry(before, label.get().getString(), label, onActivate, null,
                       tooltip, WidgetKind.CYCLE, null);
    }

    public static Entry buttonBefore(String before, Supplier<Component> label,
                                     Runnable onActivate,
                                     Supplier<Component> tooltip) {
      return new Entry(before, label.get().getString(), label, onActivate, null,
                       tooltip, WidgetKind.CYCLE, null);
    }

    public static Entry customBefore(String before, String dedupeText,
                                     Supplier<Object> customWidget,
                                     Supplier<Component> tooltip) {
      return new Entry(before, dedupeText, null, null, null, tooltip,
                       WidgetKind.CUSTOM, customWidget);
    }
  }

  public static Section detectSection(String title, String className,
                                      List<Object> renderables) {
    String cls = className == null ? "" : className.toLowerCase();
    String lowerTitle = title == null ? "" : title.toLowerCase();

    if (cls.contains("advanced") && cls.contains("graphics"))
      return Section.ADVANCED_GRAPHICS;
    if (cls.contains("advanced") && cls.contains("game") &&
        cls.contains("option"))
      return Section.ADVANCED_GAME_OPTIONS;
    if (cls.contains("advanced") &&
        (cls.contains("interface") || cls.contains("hud")))
      return Section.ADVANCED_USER_INTERFACE;
    if (cls.contains("advanced") &&
        (cls.contains("audio") || cls.contains("sound")))
      return Section.ADVANCED_AUDIO;
    if (cls.contains("graphics"))
      return Section.GRAPHICS;
    if (cls.contains("game") && cls.contains("option"))
      return Section.GAME_OPTIONS;
    if (cls.contains("interface") || cls.contains("hud"))
      return Section.USER_INTERFACE;
    if (cls.contains("audio") || cls.contains("sound"))
      return Section.AUDIO;

    if (lowerTitle.contains("advanced options") &&
        lowerTitle.contains("graphics"))
      return Section.ADVANCED_GRAPHICS;
    if (lowerTitle.contains("advanced options") && lowerTitle.contains("game"))
      return Section.ADVANCED_GAME_OPTIONS;
    if (lowerTitle.contains("advanced options") &&
        lowerTitle.contains("interface"))
      return Section.ADVANCED_USER_INTERFACE;
    if (lowerTitle.contains("advanced options") && lowerTitle.contains("audio"))
      return Section.ADVANCED_AUDIO;
    if (lowerTitle.contains("game options"))
      return Section.GAME_OPTIONS;
    if (lowerTitle.contains("user interface"))
      return Section.USER_INTERFACE;
    if (lowerTitle.equals("graphics"))
      return Section.GRAPHICS;
    if (lowerTitle.equals("audio"))
      return Section.AUDIO;

    if (RefUtil.hasAnyMessageText(renderables, "fullscreen"))
      return Section.ADVANCED_GRAPHICS;
    if (RefUtil.hasAnyMessageText(renderables, "smooth lighting",
                                  "render clouds"))
      return Section.GRAPHICS;
    if (RefUtil.hasAnyMessageText(renderables, "view bobbing", "languages"))
      return Section.GAME_OPTIONS;
    if (RefUtil.hasAnyMessageText(renderables, "maps with coordinates",
                                  "legacy creative block placing",
                                  "allow unfocused input", "vanilla tutorial"))
      return Section.ADVANCED_GAME_OPTIONS;
    if (RefUtil.hasAnyMessageText(renderables, "display hud",
                                  "attack indicator", "display game messages"))
      return Section.USER_INTERFACE;
    if (RefUtil.hasAnyMessageText(
            renderables, "display system messages as overlay",
            "display chat indicators", "show screenshot toasts",
            "autosave countdown", "selected item tooltip ellipsis"))
      return Section.ADVANCED_USER_INTERFACE;
    if (RefUtil.hasAnyMessageText(renderables, "entity shadows",
                                  "override terrain fog start",
                                  "terrain fog start", "terrain fog end"))
      return Section.ADVANCED_GRAPHICS;
    if (RefUtil.hasAnyMessageText(renderables, "fov", "render distance",
                                  "simulation distance", "brightness"))
      return Section.GRAPHICS;
    if (RefUtil.hasAnyMessageText(renderables, "mipmap levels", "vsync",
                                  "maximum framerate"))
      return Section.ADVANCED_GRAPHICS;
    if (RefUtil.hasAnyMessageText(renderables, "back sound",
                                  "hover focus sound",
                                  "inventory hover focus sound",
                                  "directional audio", "music frequency"))
      return Section.ADVANCED_AUDIO;
    if (RefUtil.hasAnyMessageText(renderables, "players", "blocks",
                                  "hostile creatures"))
      return Section.AUDIO;

    return Section.OTHER;
  }

  public static boolean matchesSectionWithLegacyMerge(Section current,
                                                      Section target) {
    if (isLegacySettingsMenusEnabled()) {
      if (target == Section.GRAPHICS)
        return current == legacyMenuTarget(target);
      if (target == Section.GAME_OPTIONS)
        return current == legacyMenuTarget(target);
      if (target == Section.AUDIO)
        return current == legacyMenuTarget(target);
      if (target == Section.USER_INTERFACE)
        return current == legacyMenuTarget(target);
    }
    return current == target;
  }

  private static Section legacyMenuTarget(Section target) {
    if (ModSettingsConfig.get().showInLegacySettings)
      return target;
    Section advanced = advancedSectionFor(target);
    return advanced == null ? target : advanced;
  }

  private static Section advancedSectionFor(Section section) {
    return switch (section) {
      case GRAPHICS -> Section.ADVANCED_GRAPHICS;
      case GAME_OPTIONS -> Section.ADVANCED_GAME_OPTIONS;
      case AUDIO -> Section.ADVANCED_AUDIO;
      case USER_INTERFACE -> Section.ADVANCED_USER_INTERFACE;
      default -> null;
    };
  }

  private static boolean shouldSkipMainLegacySection(Section section) {
    return isLegacySettingsMenusEnabled() &&
           !ModSettingsConfig.get().showInLegacySettings &&
           advancedSectionFor(section) != null;
  }

  public static boolean isLegacySettingsMenusEnabled() {
    return RefUtil.legacySettingsMenusEnabled();
  }

  public static boolean matchesUserInterfaceSection(Section section) {
    return matchesSectionWithLegacyMerge(section, Section.USER_INTERFACE);
  }

  public static List<Entry> collectEntries(Section section) {
    List<Entry> entries = new ArrayList<>();
    if (shouldSkipMainLegacySection(section))
      return entries;

    addNativeConfigEntry(section, entries);

    IrisCompat.addEntries(section, entries);
    BobbyCompat.addEntries(section, entries);
    LambDynLightsCompat.addEntries(section, entries);
    LambdaBetterGrassCompat.addEntries(section, entries);
    ContinuityCompat.addEntries(section, entries);
    NotEnoughAnimationsCompat.addEntries(section, entries);
    XaeroMinimapCompat.addEntries(section, entries);
    ZoomCompat.addEntries(section, entries);
    ChatHeadsCompat.addEntries(section, entries);
    AdvancementScreenshotCompat.addEntries(section, entries);
    PresenceFootstepsCompat.addEntries(section, entries);
    LocatorLodestonesCompat.addEntries(section, entries);

    return entries;
  }

  private static void addNativeConfigEntry(Section section,
                                           List<Entry> entries) {
    if (!matchesUserInterfaceSection(section))
      return;

    entries.add(Entry.buttonBefore(
        "legacy settings|show screenshot toasts|autosave countdown",
        () -> Component.literal("Unified Legacy Settings"),
        () -> {
          Minecraft minecraft = Minecraft.getInstance();
          minecraft.setScreen(NativeConfigScreen.create(minecraft.screen));
        },
        () -> Component.literal(
            "Configure Unified Legacy Settings integrations.")));
  }
}
