package com.mtxcore.unifiedlegacysettings.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class UnifiedLegacySettingsMixinPlugin implements IMixinConfigPlugin {

  private static final String LOCATOR_WAYPOINT_MIXIN =
      "com.mtxcore.unifiedlegacysettings.mixin.MixinLocatorWaypointTracking";

  @Override
  public void onLoad(String mixinPackage) {}

  @Override
  public String getRefMapperConfig() { return null; }

  @Override
  public boolean shouldApplyMixin(String targetClassName,
                                  String mixinClassName) {
    if (LOCATOR_WAYPOINT_MIXIN.equals(mixinClassName)) {
      return FabricLoader.getInstance().isModLoaded("locator_lodestones");
    }
    return true;
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

  @Override
  public List<String> getMixins() { return null; }

  @Override
  public void preApply(String targetClassName, ClassNode targetClass,
                       String mixinClassName, IMixinInfo mixinInfo) {}

  @Override
  public void postApply(String targetClassName, ClassNode targetClass,
                        String mixinClassName, IMixinInfo mixinInfo) {}
}
