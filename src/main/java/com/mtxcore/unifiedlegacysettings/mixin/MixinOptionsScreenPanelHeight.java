package com.mtxcore.unifiedlegacysettings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "wily.legacy.client.screen.OptionsScreen", remap = false)
public abstract class MixinOptionsScreenPanelHeight {

  @Inject(method = "getLegacyPanelHeight", at = @At("RETURN"),
          cancellable = true, remap = false)
  private void unifiedLegacySettings$keepAdvancedUiPanelStable(
      int fallbackHeight, boolean clamp, CallbackInfoReturnable<Integer> cir) {
    cir.setReturnValue(Math.min(fallbackHeight, cir.getReturnValue()));
  }
}
