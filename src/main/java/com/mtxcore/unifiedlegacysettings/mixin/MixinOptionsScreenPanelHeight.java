package com.mtxcore.unifiedlegacysettings.mixin;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "wily.legacy.client.screen.OptionsScreen", remap = false)
public abstract class MixinOptionsScreenPanelHeight {

  @Unique
  private static final String[] ULS_SECTION_MARKERS = {
      "advanced graphical effects",
      "extended online render distance",
      "grass detail",
      "snow layer blending",
      "connected textures",
      "emissive textures",
      "third-person animations",
      "dynamic lighting",
      "minimap",
      "zoom",
      "locator compass",
      "presence footsteps",
      "chat portraits",
      "take screenshot on achievement",
      "screenshot taken message",
      "screenshot delay",
      "unified legacy settings"
  };

  @Unique
  private Integer unifiedLegacySettings$lockedPanelHeight;

  @Inject(method = "init", at = @At("HEAD"), remap = false, require = 0)
  private void unifiedLegacySettings$resetLockedPanelHeight(
      org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
    unifiedLegacySettings$lockedPanelHeight = null;
  }

  @Inject(method = "getLegacyPanelHeight", at = @At("RETURN"),
          cancellable = true, remap = false)
  private void unifiedLegacySettings$keepAdvancedUiPanelStable(
      int fallbackHeight, boolean clamp, CallbackInfoReturnable<Integer> cir) {
    List<?> renderables = unifiedLegacySettings$getRenderables();
    boolean hasUlsOptions =
        unifiedLegacySettings$containsAnyMessage(renderables,
                                                 ULS_SECTION_MARKERS);
    if (!hasUlsOptions) {
      return;
    }

    int returnedHeight = cir.getReturnValue();
    int stableHeight = Math.min(fallbackHeight, returnedHeight);
    if (unifiedLegacySettings$lockedPanelHeight == null ||
        stableHeight < unifiedLegacySettings$lockedPanelHeight) {
      unifiedLegacySettings$lockedPanelHeight = stableHeight;
    }

    cir.setReturnValue(unifiedLegacySettings$lockedPanelHeight);
  }

  @Unique
  private List<?> unifiedLegacySettings$getRenderables() {
    try {
      Field rvlField =
          unifiedLegacySettings$findFieldInHierarchy(this.getClass(),
                                                     "renderableVList");
      if (rvlField == null) {
        return List.of();
      }
      rvlField.setAccessible(true);
      Object renderableVList = rvlField.get(this);
      if (renderableVList == null) {
        return List.of();
      }

      Field renderablesField = renderableVList.getClass().getField("renderables");
      Object renderables = renderablesField.get(renderableVList);
      return renderables instanceof List<?> list ? list : List.of();
    } catch (Exception ignored) {
      return List.of();
    }
  }

  @Unique
  private static boolean unifiedLegacySettings$containsAnyMessage(
      List<?> renderables, String[] needles) {
    for (Object renderable : renderables) {
      Component message = unifiedLegacySettings$getWidgetMessage(renderable);
      if (message == null) {
        continue;
      }
      String text = message.getString();
      if (text == null || text.isBlank()) {
        continue;
      }
      String lowered = text.toLowerCase(Locale.ROOT);
      for (String needle : needles) {
        if (lowered.contains(needle)) {
          return true;
        }
      }
    }
    return false;
  }

  @Unique
  private static Component unifiedLegacySettings$getWidgetMessage(Object widget) {
    try {
      return (Component)widget.getClass().getMethod("getMessage").invoke(widget);
    } catch (Exception ignored) {
    }
    try {
      Field messageField =
          unifiedLegacySettings$findFieldInHierarchy(widget.getClass(), "message");
      if (messageField == null) {
        return null;
      }
      messageField.setAccessible(true);
      Object value = messageField.get(widget);
      return value instanceof Component component ? component : null;
    } catch (Exception ignored) {
      return null;
    }
  }

  @Unique
  private static Field unifiedLegacySettings$findFieldInHierarchy(
      Class<?> type, String name) {
    while (type != null) {
      try {
        return type.getDeclaredField(name);
      } catch (NoSuchFieldException ignored) {
        type = type.getSuperclass();
      }
    }
    return null;
  }
}
