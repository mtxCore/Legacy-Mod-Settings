package com.mtxcore.legacymodsettings;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LegacyModSettings implements ClientModInitializer {

  public static final String MOD_ID = "legacy-mod-settings";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitializeClient() {
    ModSettingsConfig.get();
    TexturePackCommand.register();
    LmsDebugCommand.register();
    RuntimeCompatEnforcer.register();
    LOGGER.info("[Legacy Mod Settings] Initialized");
  }
}
