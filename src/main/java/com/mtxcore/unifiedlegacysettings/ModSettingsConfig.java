package com.mtxcore.unifiedlegacysettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;

// Global Config
public final class ModSettingsConfig {

  public enum IrisShaderMode {
    LAST_USED,
    SPECIFIC,
  }

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().create();

  private static final Path FILE =
      FabricLoader.getInstance().getConfigDir().resolve(
          "unified_legacy_settings.json");
  private static final Path PREVIOUS_FILE =
      FabricLoader.getInstance().getConfigDir().resolve(
          String.join("_", "legacy", "mod", "settings") + ".json");

  private static volatile ModSettingsConfig instance;

  private ModSettingsConfig() {}

  public boolean showAdvancedGraphicalEffects = true;

  @SerializedName(value = "showInLegacySettings",
                  alternate = {"showIrisInLegacySettings"})
  public boolean showInLegacySettings = false;

  public boolean showBobbyOptions = true;

  public boolean showDynamicLighting = true;

  public boolean showGrassDetail = true;

  public boolean showSnowLayerBlending = true;

  public boolean showContinuity = true;

  public boolean showNeaAnimations = true;

  public boolean showZoom = true;

  public boolean showLocatorLodestones = true;

  public boolean showPresenceFootsteps = true;

  public boolean showChatHeads = true;

  public boolean showXaeroMinimap = true;

  public Boolean xaeroMinimapEnabled = null;

  public Boolean zoomEnabled = null;

  public Boolean locatorCompassEnabled = null;

  public Boolean continuityConnectedTexturesEnabled = null;

  public IrisShaderMode irisShaderMode = IrisShaderMode.LAST_USED;

  public String irisSpecificShader = "";

  public boolean debugCompatLogs = false;

  public static ModSettingsConfig get() {
    if (instance == null) {
      synchronized (ModSettingsConfig.class) {
        if (instance == null) {
          instance = load();
        }
      }
    }
    return instance;
  }

  public static void mutateAndSave(Consumer<ModSettingsConfig> mutation) {
    synchronized (ModSettingsConfig.class) {
      mutation.accept(get());
      save();
    }
  }

  private static ModSettingsConfig load() {
    Path source = Files.exists(FILE) ? FILE : PREVIOUS_FILE;
    if (!Files.exists(source)) {
      ModSettingsConfig cfg = new ModSettingsConfig();
      cfg.saveToFile();
      return cfg;
    }
    try {
      ModSettingsConfig cfg;
      try (Reader reader = Files.newBufferedReader(source)) {
        cfg = GSON.fromJson(reader, ModSettingsConfig.class);
      }
      if (cfg == null)
        return new ModSettingsConfig();
      boolean migrated = !source.equals(FILE);
      boolean changed = cfg.sanitise();
      if (migrated || changed)
        cfg.saveToFile();
      return cfg;
    } catch (IOException | JsonSyntaxException e) {
      UnifiedLegacySettings.LOGGER.error(
          "[ULS] Failed to load config, using defaults", e);
      return new ModSettingsConfig();
    }
  }

  public static void save() { get().saveToFile(); }

  private void saveToFile() {
    try {
      Files.createDirectories(FILE.getParent());
      Files.writeString(FILE, GSON.toJson(this));
    } catch (IOException e) {
      UnifiedLegacySettings.LOGGER.error(
          "[ULS] Failed to save config", e);
    }
  }

  private boolean sanitise() {
    boolean changed = false;
    if (irisShaderMode == null) {
      irisShaderMode = IrisShaderMode.LAST_USED;
      changed = true;
    }
    if (irisSpecificShader == null) {
      irisSpecificShader = "";
      changed = true;
    }
    return changed;
  }
}
