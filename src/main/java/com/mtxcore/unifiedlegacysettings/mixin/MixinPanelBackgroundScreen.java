package com.mtxcore.unifiedlegacysettings.mixin;

import com.mtxcore.unifiedlegacysettings.OptionScreenEntryInjector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "wily.legacy.client.screen.PanelBackgroundScreen", remap = false)
public abstract class MixinPanelBackgroundScreen {

  @Inject(method = "panelInit", at = @At("HEAD"), remap = false, require = 0)
  private void unifiedLegacySettings$injectOptionsBeforePanelSizing(
      CallbackInfo ci) {
    OptionScreenEntryInjector.inject(this);
  }
}
