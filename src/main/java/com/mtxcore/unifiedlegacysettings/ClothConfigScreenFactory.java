package com.mtxcore.unifiedlegacysettings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ClothConfigScreenFactory {

  private static final String NO_SHADER = "(none)";

  private ClothConfigScreenFactory() {}

  static Screen create(Screen parent) {
    ModSettingsConfig cfg = ModSettingsConfig.get();

    ConfigBuilder builder =
        ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.literal("Unified Legacy Settings"))
            .setSavingRunnable(ModSettingsConfig::save);

    ConfigEntryBuilder eb = builder.entryBuilder();
    ConfigCategory category =
        builder.getOrCreateCategory(Component.literal("Integrations"));

    var graphics = eb.startSubCategory(Component.literal("Graphics Screen"));
    graphics.setExpanded(false);
    graphics.add(
        boolEntry(eb, "Advanced Graphical Effects (Iris)",
                  cfg.showAdvancedGraphicalEffects,
                  "Adds the shader on/off toggle to the Graphics screen.",
                  v -> cfg.showAdvancedGraphicalEffects = v));
    graphics.add(boolEntry(
        eb, "Show In Legacy Settings Menus", cfg.showInLegacySettings,
        "When Legacy Settings Menus are split, show integration toggles in "
            + "their matching sections.",
        v -> cfg.showInLegacySettings = v));
    category.addEntry(graphics.build());

    var advancedGraphics =
        eb.startSubCategory(Component.literal("Advanced Graphics Screen"));
    advancedGraphics.setExpanded(false);
    advancedGraphics.add(boolEntry(
        eb, "Extended Online Render Distance (Bobby)", cfg.showBobbyOptions,
        "Adds Bobby's extended-distance toggle in Advanced Graphics.",
        v -> cfg.showBobbyOptions = v));
    advancedGraphics.add(
        boolEntry(eb, "Grass Detail (LambdaBetterGrass)", cfg.showGrassDetail,
                  "Adds the Grass Detail slider in Advanced Graphics.",
                  v -> cfg.showGrassDetail = v));
    advancedGraphics.add(
        boolEntry(eb, "Snow Layer Blending (LambdaBetterGrass)",
                  cfg.showSnowLayerBlending,
                  "Adds the Snow Layer Blending toggle in Advanced Graphics.",
                  v -> cfg.showSnowLayerBlending = v));
    advancedGraphics.add(boolEntry(
        eb, "Connected + Emissive Textures (Continuity)", cfg.showContinuity,
        "Adds Continuity texture toggles in Advanced Graphics.",
        v -> cfg.showContinuity = v));
    advancedGraphics.add(boolEntry(
        eb, "Third-Person Animations (NotEnoughAnimations)",
        cfg.showNeaAnimations,
        "Adds the Third-Person Animations toggle in Advanced Graphics.",
        v -> cfg.showNeaAnimations = v));
    category.addEntry(advancedGraphics.build());

    var gameOptions =
        eb.startSubCategory(Component.literal("Game Options Screen"));
    gameOptions.setExpanded(false);
    gameOptions.add(boolEntry(
        eb, "Dynamic Lighting (LambDynamicLights)", cfg.showDynamicLighting,
        "Adds the Dynamic Lighting toggle in Game Options.",
        v -> cfg.showDynamicLighting = v));
    gameOptions.add(boolEntry(eb, "Minimap (Xaero)", cfg.showXaeroMinimap,
                              "Adds the Minimap toggle in Game Options.",
                              v -> cfg.showXaeroMinimap = v));
    category.addEntry(gameOptions.build());

    var advancedGameOptions =
        eb.startSubCategory(Component.literal("Advanced Game Options Screen"));
    advancedGameOptions.setExpanded(false);
    advancedGameOptions.add(
        boolEntry(eb, "Zoom (Zoomify)", cfg.showZoom,
                  "Adds the Zoom toggle in Advanced Game Options.",
                  v -> cfg.showZoom = v));
    advancedGameOptions.add(boolEntry(
        eb, "Locator Compass (Locator Lodestones)", cfg.showLocatorLodestones,
        "Adds a master Locator Compass toggle in Advanced Game Options.",
        v -> cfg.showLocatorLodestones = v));
    category.addEntry(advancedGameOptions.build());

    var ui = eb.startSubCategory(
        Component.literal("Advanced User Interface Screen"));
    ui.setExpanded(false);
    ui.add(
        boolEntry(eb, "Chat Portraits (Chat Heads)", cfg.showChatHeads,
                  "Adds the Chat Portraits toggle in Advanced User Interface.",
                  v -> cfg.showChatHeads = v));
    ui.add(boolEntry(
        eb, "Show Advancement Screenshot Controls",
        cfg.showAdvancementScreenshot,
        "Adds Advancement Screenshot controls in Advanced User Interface.",
        v -> cfg.showAdvancementScreenshot = v));
    ui.add(boolEntry(
        eb, "Take Screenshot on Achievement",
        cfg.advancementScreenshotEnabled,
        "Allows Advancement Screenshot to save screenshots when achievements "
            + "or advancements appear.",
        AdvancementScreenshotCompat::setTakeScreenshotOnAchievement));
    category.addEntry(ui.build());

    var audio = eb.startSubCategory(Component.literal("Audio Screen"));
    audio.setExpanded(false);
    audio.add(boolEntry(eb, "Presence Footsteps", cfg.showPresenceFootsteps,
                        "Adds the Presence Footsteps toggle in Audio.",
                        v -> cfg.showPresenceFootsteps = v));
    category.addEntry(audio.build());

    var iris = eb.startSubCategory(Component.literal("Iris Shader Settings"));
    iris.setExpanded(false);

    boolean useLast =
        cfg.irisShaderMode == ModSettingsConfig.IrisShaderMode.LAST_USED;
    iris.add(eb.startBooleanToggle(Component.literal("Use Last Used Shader"),
                                   useLast)
                 .setDefaultValue(true)
                 .setTooltip(
                     Component.literal("Re-enable the shader pack that "
                                       + "was active last time Iris was on."))
                 .setSaveConsumer(saveAnd(value -> {
                   cfg.irisShaderMode =
                       value ? ModSettingsConfig.IrisShaderMode.LAST_USED
                             : ModSettingsConfig.IrisShaderMode.SPECIFIC;
                   if (!value && (cfg.irisSpecificShader == null ||
                                  cfg.irisSpecificShader.isBlank())) {
                     List<String> packs = IrisCompat.listShaderPacks();
                     cfg.irisSpecificShader =
                         packs.isEmpty() ? "" : packs.get(0);
                   }
                 }))
                 .build());

    String[] shaderChoices = buildShaderChoices();
    String currentShader = resolveCurrentShaderChoice(cfg, shaderChoices);
    iris.add(
        eb.startSelector(Component.literal("Shader To Use"), shaderChoices,
                         currentShader)
            .setNameProvider(Component::literal)
            .setDefaultValue(NO_SHADER)
            .setTooltip(Component.literal("Which shader pack to enable when "
                                          + "'Use Last Used Shader' is off."))
            .setSaveConsumer(saveAnd(value -> {
              if (value == null || value.equals(NO_SHADER)) {
                cfg.irisSpecificShader = "";
                return;
              }
              cfg.irisSpecificShader = value;
              cfg.irisShaderMode = ModSettingsConfig.IrisShaderMode.SPECIFIC;
            }))
            .build());

    category.addEntry(iris.build());

    return builder.build();
  }

  private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?>
  boolEntry(ConfigEntryBuilder eb, String label, boolean current,
            String tooltip, Consumer<Boolean> mutation) {
    return eb.startBooleanToggle(Component.literal(label), current)
        .setDefaultValue(true)
        .setTooltip(Component.literal(tooltip))
        .setSaveConsumer(saveAnd(mutation))
        .build();
  }

  private static <T> Consumer<T> saveAnd(Consumer<T> mutation) {
    return value
        -> ModSettingsConfig.mutateAndSave(cfg -> mutation.accept(value));
  }

  private static String[] buildShaderChoices() {
    List<String> choices = new ArrayList<>();
    choices.add(NO_SHADER);
    choices.addAll(IrisCompat.listShaderPacks());
    return choices.toArray(new String[0]);
  }

  private static String resolveCurrentShaderChoice(ModSettingsConfig cfg,
                                                   String[] choices) {
    String selected =
        (cfg.irisSpecificShader == null || cfg.irisSpecificShader.isBlank())
            ? NO_SHADER
            : cfg.irisSpecificShader;
    for (String choice : choices) {
      if (choice.equals(selected))
        return selected;
    }
    return NO_SHADER;
  }
}
