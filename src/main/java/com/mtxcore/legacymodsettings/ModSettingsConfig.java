package com.mtxcore.legacymodsettings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.fabricmc.loader.api.FabricLoader;

public final class ModSettingsConfig {

  public enum IrisShaderMode {
    LAST_USED,
    SPECIFIC,
  }

  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().create();

  private static final Path FILE =
      FabricLoader.getInstance().getConfigDir().resolve(
          "legacy_mod_settings.json");

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
    if (!Files.exists(FILE)) {
      ModSettingsConfig cfg = new ModSettingsConfig();
      cfg.saveToFile();
      return cfg;
    }
    try {
      ModSettingsConfig cfg =
          GSON.fromJson(Files.newBufferedReader(FILE), ModSettingsConfig.class);
      if (cfg == null)
        return new ModSettingsConfig();
      cfg.sanitise();
      return cfg;
    } catch (IOException | JsonSyntaxException e) {
      LegacyModSettings.LOGGER.error(
          "[Legacy Mod Settings] Failed to load config, using defaults", e);
      return new ModSettingsConfig();
    }
  }

  public static void save() { get().saveToFile(); }

  private void saveToFile() {
    try {
      Files.createDirectories(FILE.getParent());
      Files.writeString(FILE, GSON.toJson(this));
    } catch (IOException e) {
      LegacyModSettings.LOGGER.error(
          "[Legacy Mod Settings] Failed to save config", e);
    }
  }

  private void sanitise() {
    if (irisShaderMode == null)
      irisShaderMode = IrisShaderMode.LAST_USED;
    if (irisSpecificShader == null)
      irisSpecificShader = "";
  }
}
