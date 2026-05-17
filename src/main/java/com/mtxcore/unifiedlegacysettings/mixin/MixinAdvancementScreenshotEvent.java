package com.mtxcore.unifiedlegacysettings.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mtxcore.unifiedlegacysettings.AdvancementScreenshotCompat;
import java.io.File;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
    targets = "com.natamus.advancementscreenshot_common_fabric.events.AdvancementGetEvent",
    remap = false)
public abstract class MixinAdvancementScreenshotEvent {
  @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true,
          remap = false, require = 1)
  private static void unifiedLegacySettings$blockQueuedScreenshots(
      Minecraft mc, CallbackInfo ci) {
    if (AdvancementScreenshotCompat.shouldTakeScreenshotOnAchievement())
      return;

    AdvancementScreenshotCompat.clearPendingScreenshot();
    ci.cancel();
  }

  @Redirect(
      method = "onClientTick",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/Screenshot;grab(Ljava/io/File;Lcom/mojang/blaze3d/pipeline/RenderTarget;Ljava/util/function/Consumer;)V"),
      require = 1)
  private static void unifiedLegacySettings$guardScreenshotGrab(
      File gameDirectory, RenderTarget renderTarget,
      Consumer<Component> messageReceiver) {
    if (!AdvancementScreenshotCompat.shouldTakeScreenshotOnAchievement()) {
      AdvancementScreenshotCompat.clearPendingScreenshot();
      return;
    }

    Screenshot.grab(gameDirectory, renderTarget, messageReceiver);
  }
}
