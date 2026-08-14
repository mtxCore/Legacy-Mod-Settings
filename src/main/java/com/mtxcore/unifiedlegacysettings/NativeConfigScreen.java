package com.mtxcore.unifiedlegacysettings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;

public final class NativeConfigScreen extends PanelVListScreen {

  private static final int PANEL_WIDTH = 250;
  private static final int PANEL_HEIGHT = 215;
  private static final String NO_SHADER = "(none)";

  private NativeConfigScreen(Screen parent) {
    super(parent, screen -> Panel.centered(screen, PANEL_WIDTH, PANEL_HEIGHT,
                                           0, 20),
          Component.literal("Unified Legacy Settings"));
    buildRows();
  }

  public static Screen create(Screen parent) {
    return new NativeConfigScreen(parent);
  }

  @Override
  public void onClose() {
    ModSettingsConfig.save();
    super.onClose();
  }

  @Override
  public void renderableVListInit() {
    renderableVList.init(panel.getX() + 10, panel.getY() + 10,
                         panel.getWidth() - 20, panel.getHeight() - 20);
  }

  private void buildRows() {
    ModSettingsConfig cfg = ModSettingsConfig.get();

    category("Overview");
    info("Installed Integrations: " + IntegrationCatalog.installedCount() +
         " / " + IntegrationCatalog.totalCount());
    info("Visible Installed Entries: " +
         IntegrationCatalog.visibleInstalledCount(cfg) + " / " +
         IntegrationCatalog.installedCount());

    category("Placement");
    toggle("Show In Main Legacy Settings Menus", () -> cfg.showInLegacySettings,
           v -> cfg.showInLegacySettings = v,
           "Show Unified Legacy Settings entries on main Legacy category screens.");
    toggle("Hide Missing Integrations In Config",
           () -> cfg.hideMissingIntegrationsInConfig,
           v -> cfg.hideMissingIntegrationsInConfig = v,
           "Hide config controls for integrations whose mods are not installed.");
    toggle("Show Installed/Missing Labels",
           () -> cfg.showIntegrationStatusInConfig,
           v -> cfg.showIntegrationStatusInConfig = v,
           "Add installation status labels to integration controls.");

    category("Bulk Actions");
    action("Show All Integration Entries",
           () -> IntegrationCatalog.applyVisibilityPreset(
               ModSettingsConfig.get(),
               IntegrationCatalog.VisibilityPreset.ALL),
           "Enable every Unified Legacy Settings integration entry.");
    action("Show Installed Integration Entries",
           () -> IntegrationCatalog.applyVisibilityPreset(
               ModSettingsConfig.get(),
               IntegrationCatalog.VisibilityPreset.INSTALLED_ONLY),
           "Enable entries only for integrations currently installed.");
    action("Hide All Integration Entries",
           () -> IntegrationCatalog.applyVisibilityPreset(
               ModSettingsConfig.get(),
               IntegrationCatalog.VisibilityPreset.NONE),
           "Hide every integration entry from Legacy4J settings menus.");
    action("Reset Remembered Runtime States",
           () -> IntegrationCatalog.clearRememberedRuntimeStates(
               ModSettingsConfig.get()),
           "Clear saved on/off states for runtime-managed integrations.");

    category("Graphics");
    integrationToggle("Advanced Graphical Effects (Iris)",
           new String[] {"iris"},
           () -> cfg.showAdvancedGraphicalEffects,
           v -> cfg.showAdvancedGraphicalEffects = v,
           "Adds the shader on/off toggle to the Graphics screen.");

    category("Advanced Graphics");
    integrationToggle("Extended Online Render Distance (Bobby)",
           new String[] {"bobby"},
           () -> cfg.showBobbyOptions, v -> cfg.showBobbyOptions = v,
           "Adds Bobby's extended-distance toggle in Advanced Graphics.");
    integrationToggle("Grass Detail (LambdaBetterGrass)",
           new String[] {"lambdabettergrass"}, () -> cfg.showGrassDetail,
           v -> cfg.showGrassDetail = v,
           "Adds the Grass Detail slider in Advanced Graphics.");
    integrationToggle("Snow Layer Blending (LambdaBetterGrass)",
           new String[] {"lambdabettergrass"},
           () -> cfg.showSnowLayerBlending,
           v -> cfg.showSnowLayerBlending = v,
           "Adds the Snow Layer Blending toggle in Advanced Graphics.");
    integrationToggle("Connected + Emissive Textures (Continuity)",
           new String[] {"continuity"},
           () -> cfg.showContinuity, v -> cfg.showContinuity = v,
           "Adds Continuity texture toggles in Advanced Graphics.");
    integrationToggle("Third-Person Animations (NotEnoughAnimations)",
           new String[] {"notenoughanimations"},
           () -> cfg.showNeaAnimations, v -> cfg.showNeaAnimations = v,
           "Adds the Third-Person Animations toggle in Advanced Graphics.");

    category("Game Options");
    integrationToggle("Dynamic Lighting (LambDynamicLights)",
           new String[] {"lambdynlights"},
           () -> cfg.showDynamicLighting,
           v -> cfg.showDynamicLighting = v,
           "Adds the Dynamic Lighting toggle in Game Options.");
    integrationToggle("Minimap (Xaero)", new String[] {"xaerominimap"},
           () -> cfg.showXaeroMinimap,
           v -> cfg.showXaeroMinimap = v,
           "Adds the Minimap toggle in Game Options.");

    category("Advanced Game Options");
    integrationToggle("Zoom (Zoomify)", new String[] {"zoomify"},
           () -> cfg.showZoom, v -> cfg.showZoom = v,
           "Adds the Zoom toggle in Advanced Game Options.");
    integrationToggle("Locator Compass (Locator Lodestones)",
           new String[] {"locator_lodestones"},
           () -> cfg.showLocatorLodestones,
           v -> cfg.showLocatorLodestones = v,
           "Adds a master Locator Compass toggle in Advanced Game Options.");

    category("User Interface");
    integrationToggle("Chat Portraits (Chat Heads)",
           new String[] {"chat_heads", "chatheads"}, () -> cfg.showChatHeads,
           v -> cfg.showChatHeads = v,
           "Adds the Chat Portraits toggle in User Interface.");
    integrationToggle("Show Advancement Screenshot Controls",
           new String[] {"advancementscreenshot"},
           () -> cfg.showAdvancementScreenshot,
           v -> cfg.showAdvancementScreenshot = v,
           "Adds Advancement Screenshot controls in User Interface.");
    if (IntegrationCatalog.shouldShowConfigRow(
            cfg, "advancementscreenshot")) {
      toggle(IntegrationCatalog.label(
                 cfg, "Take Screenshot on Achievement",
                 "advancementscreenshot"),
             () -> cfg.advancementScreenshotEnabled,
             AdvancementScreenshotCompat::setTakeScreenshotOnAchievement,
             "Allows Advancement Screenshot to save screenshots when achievements appear.");
    }

    category("Audio");
    integrationToggle("Presence Footsteps",
           new String[] {"presencefootsteps"}, () -> cfg.showPresenceFootsteps,
           v -> cfg.showPresenceFootsteps = v,
           "Adds the Presence Footsteps toggle in Audio.");

    category("Iris Shader Settings");
    toggle("Use Last Used Shader",
           () -> cfg.irisShaderMode ==
                 ModSettingsConfig.IrisShaderMode.LAST_USED,
           value -> {
             cfg.irisShaderMode =
                 value ? ModSettingsConfig.IrisShaderMode.LAST_USED
                       : ModSettingsConfig.IrisShaderMode.SPECIFIC;
             if (!value && (cfg.irisSpecificShader == null ||
                            cfg.irisSpecificShader.isBlank())) {
               List<String> packs = IrisCompat.listShaderPacks();
               cfg.irisSpecificShader = packs.isEmpty() ? "" : packs.get(0);
             }
           },
           "Re-enable the shader pack that was active last time Iris was on.");
    cycle("Shader To Use", this::shaderChoices,
          () -> resolveCurrentShaderChoice(cfg, shaderChoices()),
          value -> {
            if (value == null || value.equals(NO_SHADER)) {
              cfg.irisSpecificShader = "";
              return;
            }
            cfg.irisSpecificShader = value;
            cfg.irisShaderMode = ModSettingsConfig.IrisShaderMode.SPECIFIC;
          },
          "Which shader pack to enable when Use Last Used Shader is off.");
  }

  private void category(String title) {
    renderableVList.addCategory(Component.literal(title));
  }

  private void toggle(String label, BooleanSupplier selected,
                      Consumer<Boolean> save, String tooltip) {
    renderableVList.addRenderable(LegacyWidgetFactory.tickBox(
        0, 0, 200, () -> Component.literal(label), selected,
        value -> ModSettingsConfig.mutateAndSave(cfg -> save.accept(value)),
        () -> Component.literal(tooltip)));
  }

  private void integrationToggle(String label, String[] modIds,
                                 BooleanSupplier selected,
                                 Consumer<Boolean> save, String tooltip) {
    ModSettingsConfig cfg = ModSettingsConfig.get();
    if (!IntegrationCatalog.shouldShowConfigRow(cfg, modIds))
      return;
    toggle(IntegrationCatalog.label(cfg, label, modIds), selected, save,
           tooltip);
  }

  private void action(String label, Runnable save, String tooltip) {
    AbstractWidget button = LegacyWidgetFactory.button(
        0, 0, 200, 20, Component.literal(label), b -> {
          ModSettingsConfig.mutateAndSave(cfg -> save.run());
          Minecraft minecraft = Minecraft.getInstance();
          RefUtil.setScreen(
              minecraft,
              NativeConfigScreen.create(RefUtil.currentScreen(minecraft)));
        });
    button.setTooltip(Tooltip.create(Component.literal(tooltip)));
    renderableVList.addRenderable(button);
  }

  private void info(String label) {
    AbstractWidget row = LegacyWidgetFactory.button(
        0, 0, 200, 20, Component.literal(label), b -> {});
    row.active = false;
    renderableVList.addRenderable(row);
  }

  private void cycle(String label, OptionList choices, StringSupplier current,
                     Consumer<String> save, String tooltip) {
    AbstractWidget button = LegacyWidgetFactory.button(
        0, 0, 200, 20, cycleLabel(label, current.get()), b -> {
          String next = nextChoice(choices.get(), current.get());
          ModSettingsConfig.mutateAndSave(cfg -> save.accept(next));
          b.setMessage(cycleLabel(label, next));
        });
    button.setTooltip(Tooltip.create(Component.literal(tooltip)));
    renderableVList.addRenderable(button);
  }

  private String[] shaderChoices() {
    List<String> choices = new ArrayList<>();
    choices.add(NO_SHADER);
    choices.addAll(IrisCompat.listShaderPacks());
    return choices.toArray(new String[0]);
  }

  private static Component cycleLabel(String label, String value) {
    return Component.literal(label + ": " + value);
  }

  private static String nextChoice(String[] choices, String current) {
    if (choices.length == 0)
      return "";
    for (int i = 0; i < choices.length; i++) {
      if (choices[i].equals(current))
        return choices[(i + 1) % choices.length];
    }
    return choices[0];
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

  @FunctionalInterface
  private interface OptionList {
    String[] get();
  }

  @FunctionalInterface
  private interface StringSupplier {
    String get();
  }
}
