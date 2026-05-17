package com.mtxcore.unifiedlegacysettings;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnifiedLegacySettings implements ClientModInitializer {

  public static final String MOD_ID = "unified-legacy-settings";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitializeClient() {
    ModSettingsConfig.get();
    RuntimeCompatEnforcer.register();
    LOGGER.info("[ULS] Initialized");
  }
}
