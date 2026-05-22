package com.mtxcore.unifiedlegacysettings;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

public final class ModMenuIntegration implements ModMenuApi {

  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    if (!FabricLoader.getInstance().isModLoaded("cloth-config2")) {
      return null;
    }

    return parent -> {
      try {
        return ClothConfigScreenFactory.create((Screen)parent);
      } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
        UnifiedLegacySettings.LOGGER.error(
            "[ULS] Cloth Config screen is unavailable", e);
        return (Screen)parent;
      }
    };
  }
}
