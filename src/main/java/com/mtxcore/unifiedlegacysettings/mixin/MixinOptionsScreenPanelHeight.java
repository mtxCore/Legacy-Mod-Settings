package com.mtxcore.unifiedlegacysettings.mixin;

import com.mtxcore.unifiedlegacysettings.ModSettingsConfig;
import com.mtxcore.unifiedlegacysettings.ModSettingsCompat;
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
  private static final String[] UI_SECTION_MARKERS = {
      "display hud",
      "display hand",
      "display game messages",
      "display chat indicators",
      "show screenshot toasts",
      "autosave countdown",
      "chat portraits",
      "take screenshot on achievement",
      "screenshot taken message",
      "screenshot delay"
  };

  @Unique
  private static final String[] UI_TITLE_MARKERS = {
      "user interface",
      "interface"
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
    ModSettingsConfig cfg = ModSettingsConfig.get();
    if (!cfg.showChatHeads && !cfg.showAdvancementScreenshot) {
      return;
    }

    String title = unifiedLegacySettings$getTitleText().toLowerCase(Locale.ROOT);
    List<?> renderables = unifiedLegacySettings$getRenderables();
    ModSettingsCompat.Section section =
        unifiedLegacySettings$detectSection(title, renderables);
    boolean looksLikeUiByTitle =
        unifiedLegacySettings$containsAnySubstring(title, UI_TITLE_MARKERS);
    boolean looksLikeUiByOptions =
        unifiedLegacySettings$containsAnyMessage(renderables, UI_SECTION_MARKERS);
    if (section != ModSettingsCompat.Section.ADVANCED_USER_INTERFACE &&
        !looksLikeUiByTitle && !looksLikeUiByOptions) {
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
  @SuppressWarnings("unchecked")
  private ModSettingsCompat.Section unifiedLegacySettings$detectSection(
      String title, List<?> renderables) {
    return ModSettingsCompat.detectSection(title, this.getClass().getName(),
                                           (List<Object>)renderables);
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
  private static boolean unifiedLegacySettings$containsAnySubstring(
      String haystack, String[] needles) {
    if (haystack == null || haystack.isBlank()) {
      return false;
    }
    for (String needle : needles) {
      if (haystack.contains(needle)) {
        return true;
      }
    }
    return false;
  }

  @Unique
  private String unifiedLegacySettings$getTitleText() {
    try {
      Object title = this.getClass().getMethod("getTitle").invoke(this);
      if (title instanceof Component c) {
        return c.getString() + " " + c;
      }
    } catch (Exception ignored) {
    }
    return "";
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
