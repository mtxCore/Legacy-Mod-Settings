package com.mtxcore.legacymodsettings.mixin;

import com.mtxcore.legacymodsettings.LocatorLodestonesCompat;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.pneumono.locator_lodestones.WaypointTracking",
       remap = false)
public abstract class MixinLocatorWaypointTracking {

  @Inject(method = "getWaypointsFromPlayer", at = @At("HEAD"),
          cancellable = true, remap = true)
  private static void
  legacyModSettings$blockWaypointsFromPlayer(
      Player player, CallbackInfoReturnable<List<?>> cir) {
    // Short-circuit at HEAD so disabled locator mode never seeds fresh player
    // waypoints.
    if (!LocatorLodestonesCompat.isRuntimeEnabled()) {
      cir.setReturnValue(Collections.emptyList());
    }
  }
}
