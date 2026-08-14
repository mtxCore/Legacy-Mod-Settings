package com.mtxcore.unifiedlegacysettings;

import java.util.List;

final class IntegrationCatalog {

  enum VisibilityPreset {
    ALL,
    INSTALLED_ONLY,
    NONE,
  }

  private IntegrationCatalog() {}

  static final List<Integration> INTEGRATIONS =
      List.of(
          new Integration(
              "Advanced Graphical Effects (Iris)", new String[] {"iris"},
              cfg -> cfg.showAdvancedGraphicalEffects,
              (cfg, value) -> cfg.showAdvancedGraphicalEffects = value),
          new Integration(
              "Extended Online Render Distance (Bobby)", new String[] {"bobby"},
              cfg -> cfg.showBobbyOptions,
              (cfg, value) -> cfg.showBobbyOptions = value),
          new Integration(
              "Grass Detail (LambdaBetterGrass)",
              new String[] {"lambdabettergrass"}, cfg -> cfg.showGrassDetail,
              (cfg, value) -> cfg.showGrassDetail = value),
          new Integration(
              "Snow Layer Blending (LambdaBetterGrass)",
              new String[] {"lambdabettergrass"},
              cfg -> cfg.showSnowLayerBlending,
              (cfg, value) -> cfg.showSnowLayerBlending = value),
          new Integration(
              "Connected + Emissive Textures (Continuity)",
              new String[] {"continuity"}, cfg -> cfg.showContinuity,
              (cfg, value) -> cfg.showContinuity = value),
          new Integration(
              "Third-Person Animations (NotEnoughAnimations)",
              new String[] {"notenoughanimations"},
              cfg -> cfg.showNeaAnimations,
              (cfg, value) -> cfg.showNeaAnimations = value),
          new Integration(
              "Dynamic Lighting (LambDynamicLights)",
              new String[] {"lambdynlights"}, cfg -> cfg.showDynamicLighting,
              (cfg, value) -> cfg.showDynamicLighting = value),
          new Integration(
              "Minimap (Xaero)", new String[] {"xaerominimap"},
              cfg -> cfg.showXaeroMinimap,
              (cfg, value) -> cfg.showXaeroMinimap = value),
          new Integration(
              "Zoom (Zoomify)", new String[] {"zoomify"}, cfg -> cfg.showZoom,
              (cfg, value) -> cfg.showZoom = value),
          new Integration(
              "Locator Compass (Locator Lodestones)",
              new String[] {"locator_lodestones"},
              cfg -> cfg.showLocatorLodestones,
              (cfg, value) -> cfg.showLocatorLodestones = value),
          new Integration(
              "Chat Portraits (Chat Heads)",
              new String[] {"chat_heads", "chatheads"},
              cfg -> cfg.showChatHeads,
              (cfg, value) -> cfg.showChatHeads = value),
          new Integration(
              "Advancement Screenshot",
              new String[] {"advancementscreenshot"},
              cfg -> cfg.showAdvancementScreenshot,
              (cfg, value) -> cfg.showAdvancementScreenshot = value),
          new Integration(
              "Presence Footsteps", new String[] {"presencefootsteps"},
              cfg -> cfg.showPresenceFootsteps,
              (cfg, value) -> cfg.showPresenceFootsteps = value));

  static int totalCount() {
    return INTEGRATIONS.size();
  }

  static int installedCount() {
    int count = 0;
    for (Integration integration : INTEGRATIONS) {
      if (integration.isInstalled())
        count++;
    }
    return count;
  }

  static int visibleCount(ModSettingsConfig cfg) {
    int count = 0;
    for (Integration integration : INTEGRATIONS) {
      if (integration.isVisible(cfg))
        count++;
    }
    return count;
  }

  static int visibleInstalledCount(ModSettingsConfig cfg) {
    int count = 0;
    for (Integration integration : INTEGRATIONS) {
      if (integration.isInstalled() && integration.isVisible(cfg))
        count++;
    }
    return count;
  }

  static void applyVisibilityPreset(ModSettingsConfig cfg,
                                    VisibilityPreset preset) {
    for (Integration integration : INTEGRATIONS) {
      boolean visible = switch (preset) {
        case ALL -> true;
        case INSTALLED_ONLY -> integration.isInstalled();
        case NONE -> false;
      };
      integration.setVisible(cfg, visible);
    }
  }

  static String label(ModSettingsConfig cfg, String label,
                      String... modIds) {
    if (!cfg.showIntegrationStatusInConfig)
      return label;
    return label + " [" + (RefUtil.isModLoaded(modIds) ? "Installed"
                                                        : "Missing") +
           "]";
  }

  static boolean shouldShowConfigRow(ModSettingsConfig cfg,
                                     String... modIds) {
    return !cfg.hideMissingIntegrationsInConfig || RefUtil.isModLoaded(modIds);
  }

  static void clearRememberedRuntimeStates(ModSettingsConfig cfg) {
    cfg.xaeroMinimapEnabled = null;
    cfg.zoomEnabled = null;
    cfg.locatorCompassEnabled = null;
    cfg.continuityConnectedTexturesEnabled = null;
    cfg.irisShaderMode = ModSettingsConfig.IrisShaderMode.LAST_USED;
    cfg.irisSpecificShader = "";
  }

  record Integration(String name, String[] modIds, FlagGetter getter,
                     FlagSetter setter) {
    boolean isInstalled() {
      return RefUtil.isModLoaded(modIds);
    }

    boolean isVisible(ModSettingsConfig cfg) {
      return getter.get(cfg);
    }

    void setVisible(ModSettingsConfig cfg, boolean visible) {
      setter.set(cfg, visible);
    }
  }

  interface FlagGetter {
    boolean get(ModSettingsConfig cfg);
  }

  interface FlagSetter {
    void set(ModSettingsConfig cfg, boolean value);
  }
}
