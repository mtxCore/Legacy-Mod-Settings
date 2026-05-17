package com.mtxcore.unifiedlegacysettings.mixin;

import com.mtxcore.unifiedlegacysettings.AdvancementScreenshotCompat;
import java.lang.reflect.Field;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AdvancementToast.class, priority = 1100)
public abstract class MixinAdvancementToastGuard {
  @Shadow private @Final AdvancementHolder advancement;

  @Inject(method = "update", at = @At("HEAD"), require = 0)
  private void unifiedLegacySettings$disableAdvancementScreenshotScheduling(
      CallbackInfo ci) {
    if (AdvancementScreenshotCompat.shouldTakeScreenshotOnAchievement())
      return;

    DisplayInfo displayInfo = (DisplayInfo)this.advancement.value().display().orElse(null);
    if (displayInfo == null)
      return;

    Component advancementTitle = displayInfo.getTitle();
    Class<?> utilClass = classForName(
        "com.natamus.advancementscreenshot_common_fabric.util.Util");
    if (utilClass == null)
      return;

    writeField(null, field(utilClass, "activeAdvancementTitle"), advancementTitle);
    writeField(null, field(utilClass, "takescreenshot"), false);
    writeField(null, field(utilClass, "cooldown"), -1);
  }

  private static Class<?> classForName(String name) {
    try {
      return Class.forName(name);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Field field(Class<?> cls, String name) {
    if (cls == null || name == null)
      return null;
    for (Class<?> cur = cls; cur != null; cur = cur.getSuperclass()) {
      try {
        Field f = cur.getDeclaredField(name);
        f.setAccessible(true);
        return f;
      } catch (NoSuchFieldException ignored) {
      }
    }
    return null;
  }

  private static void writeField(Object target, Field field, Object value) {
    if (field == null)
      return;
    try {
      field.set(target, value);
    } catch (Exception ignored) {
    }
  }
}
