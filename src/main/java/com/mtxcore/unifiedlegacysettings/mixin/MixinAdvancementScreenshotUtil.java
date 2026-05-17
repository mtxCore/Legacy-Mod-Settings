package com.mtxcore.unifiedlegacysettings.mixin;

import com.mtxcore.unifiedlegacysettings.AdvancementScreenshotCompat;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.natamus.advancementscreenshot_common_fabric.util.Util",
       remap = false)
public abstract class MixinAdvancementScreenshotUtil {

  @Inject(method = "takeScreenshot", at = @At("HEAD"), cancellable = true,
          remap = false, require = 1)
  private static void unifiedLegacySettings$toggleAchievementScreenshots(
      Component advancementTitle, CallbackInfo ci) {
    if (AdvancementScreenshotCompat.shouldTakeScreenshotOnAchievement())
      return;

    AdvancementScreenshotCompat.clearPendingScreenshot();
    ci.cancel();
  }
}
